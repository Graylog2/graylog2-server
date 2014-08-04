/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2;

import com.beust.jcommander.JCommander;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.log4j.InstrumentedAppender;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.log4j.Level;
import org.graylog2.bindings.AlarmCallbackBindings;
import org.graylog2.bindings.InitializerBindings;
import org.graylog2.bindings.MessageFilterBindings;
import org.graylog2.bindings.MessageOutputBindings;
import org.graylog2.bindings.PersistenceServicesBindings;
import org.graylog2.bindings.ServerBindings;
import org.graylog2.bindings.ServerMessageInputBindings;
import org.graylog2.cluster.NodeService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugins.PluginInstaller;
import org.graylog2.shared.NodeRunner;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.graylog2.shared.plugins.PluginLoader;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main class of Graylog2.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Main extends NodeRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String ENVIRONMENT_PREFIX = "GRAYLOG2_";
    private static final String PROPERTIES_PREFIX = "graylog2.";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // So jung kommen wir nicht mehr zusammen.

        final CommandLineArguments commandLineArguments = new CommandLineArguments();
        final JCommander jCommander = new JCommander(commandLineArguments, args);
        jCommander.setProgramName("graylog2");

        if (commandLineArguments.isShowHelp()) {
            jCommander.usage();
            System.exit(0);
        }

        if (commandLineArguments.isShowVersion()) {
            System.out.println("Graylog2 Server " + ServerVersion.VERSION);
            System.out.println("JRE: " + Tools.getSystemInformation());
            System.exit(0);
        }

        if(commandLineArguments.isDumpDefaultConfig()) {
            final JadConfig jadConfig = new JadConfig();
            jadConfig.addConfigurationBean(new Configuration());
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        final JadConfig jadConfig = new JadConfig();
        final Configuration configuration = readConfiguration(jadConfig, commandLineArguments.getConfigFile());

        if(commandLineArguments.isDumpConfig()) {
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        if (configuration.getPasswordSecret().isEmpty()) {
            LOG.error("No password secret set. Please define password_secret in your graylog2.conf.");
            System.exit(1);
        }

        if (commandLineArguments.isInstallPlugin()) {
            System.out.println("Plugin installation requested.");
            PluginInstaller installer = new PluginInstaller(
                    commandLineArguments.getPluginShortname(),
                    commandLineArguments.getPluginVersion(),
                    commandLineArguments.isForcePlugin()
            );
            
            installer.install();
            System.exit(0);
        }

        // Are we in debug mode?
        Level logLevel = Level.INFO;
        if (commandLineArguments.isDebug()) {
            LOG.info("Running in Debug mode");
            logLevel = Level.DEBUG;
        }
        org.apache.log4j.Logger.getRootLogger().setLevel(logLevel);
        org.apache.log4j.Logger.getLogger(Main.class.getPackage().getName()).setLevel(logLevel);

        PluginLoader pluginLoader = new PluginLoader(new File(configuration.getPluginDir()));
        List<PluginModule> pluginModules = Lists.newArrayList();
        for (Plugin plugin : pluginLoader.loadPlugins())
            pluginModules.addAll(plugin.modules());

        LOG.debug("Loaded modules: " + pluginModules);

        GuiceInstantiationService instantiationService = new GuiceInstantiationService();
        List<Module> bindingsModules = getBindingsModules(instantiationService,
                new ServerBindings(configuration),
                new PersistenceServicesBindings(),
                new ServerMessageInputBindings(),
                new MessageFilterBindings(),
                new AlarmCallbackBindings(),
                new InitializerBindings(),
                new MessageOutputBindings());
        LOG.debug("Adding plugin modules: " + pluginModules);
        bindingsModules.addAll(pluginModules);
        final Injector injector = Guice.createInjector(bindingsModules);
        instantiationService.setInjector(injector);

        // This is holding all our metrics.
        final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);

        // Report metrics via JMX.
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        InstrumentedAppender logMetrics = new InstrumentedAppender(metrics);
        logMetrics.activateOptions();
        org.apache.log4j.Logger.getRootLogger().addAppender(logMetrics);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        LOG.info("Graylog2 {} starting up. (JRE: {})", ServerVersion.VERSION, Tools.getSystemInformation());

        // Do not use a PID file if the user requested not to
        if (!commandLineArguments.isNoPidFile()) {
            savePidFile(commandLineArguments.getPidFile());
        }

        monkeyPatchHK2(injector);

        // Le server object. This is where all the magic happens.
        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        serverStatus.setLifecycle(Lifecycle.STARTING);

        final ActivityWriter activityWriter = injector.getInstance(ActivityWriter.class);
        final ServiceManager serviceManager = injector.getInstance(ServiceManager.class);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String msg = "SIGNAL received. Shutting down.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, Main.class));

                GracefulShutdown shutdown = injector.getInstance(GracefulShutdown.class);
                shutdown.runWithoutExit();
                serviceManager.stopAsync().awaitStopped();
            }
        });

        // Register this node.
        final NodeService nodeService = injector.getInstance(NodeService.class);
        nodeService.registerServer(serverStatus.getNodeId().toString(), configuration.isMaster(), configuration.getRestTransportUri());

        if (configuration.isMaster() && !nodeService.isOnlyMaster(serverStatus.getNodeId())) {
            LOG.warn("Detected another master in the cluster. Retrying in {} seconds to make sure it is not "
                    + "an old stale instance.", configuration.getStaleMasterTimeout());
            try {
                Thread.sleep(configuration.getStaleMasterTimeout());
            } catch (InterruptedException e) { /* nope */ }
            
            if (!nodeService.isOnlyMaster(serverStatus.getNodeId())) {
                // All devils here.
                String what = "Detected other master node in the cluster! Starting as non-master! "
                        + "This is a mis-configuration you should fix.";
                LOG.warn(what);
                activityWriter.write(new Activity(what, Main.class));

                // Write a notification.
                final NotificationService notificationService = injector.getInstance(NotificationService.class);
                Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.MULTI_MASTER)
                        .addSeverity(Notification.Severity.URGENT);
                notificationService.publishIfFirst(notification);

                configuration.setIsMaster(false);
            } else {
                LOG.warn("Stale master has gone. Starting as master.");
            }
        }
        
        // Enable local mode?
        if (commandLineArguments.isLocal() || commandLineArguments.isDebug()) {
            // In local mode, systemstats are sent to localhost for example.
            LOG.info("Running in local mode");
            serverStatus.setLocalMode(true);
        }

        // Are we in stats mode?
        if (commandLineArguments.isStats()) {
            LOG.info("Printing system utilization information.");
            serverStatus.setStatsMode(true);
        }


        if (!commandLineArguments.performRetention()) {
            configuration.setPerformRetention(false);
        }

        // propagate default size to input plugins
        MessageInput.setDefaultRecvBufferSize(configuration.getUdpRecvBufferSizes());

        // Start services.
        final ServiceManagerListener serviceManagerListener = injector.getInstance(ServiceManagerListener.class);
        serviceManager.addListener(serviceManagerListener, MoreExecutors.sameThreadExecutor());
        try {
            serviceManager.startAsync().awaitHealthy();
        } catch (Exception e) {
            try {
                serviceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
            } catch (TimeoutException timeoutException) {
                LOG.error("Unable to shutdown properly on time. {}", serviceManager.servicesByState());
            }
            LOG.error("Graylog2 startup failed. Exiting. Exception was:", e);
            System.exit(-1);
        }
        LOG.info("Services started, startup times in ms: {}", serviceManager.startupTimes());

        activityWriter.write(new Activity("Started up.", Main.class));
        LOG.info("Graylog2 up and running.");

        // Block forever.
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            return;
        }
    }

    private static String dumpConfiguration(final Map<String, String> configMap) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Configuration of graylog2-server ").append(ServerVersion.VERSION).append(System.lineSeparator());
        sb.append("# Generated on ").append(DateTime.now()).append(System.lineSeparator());

        for(Map.Entry<String, String> entry:  configMap.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append(System.lineSeparator());
        }

        return sb.toString();
    }

    private static Configuration readConfiguration(final JadConfig jadConfig, final String configFile) {
        final Configuration configuration = new Configuration();

        jadConfig.addConfigurationBean(configuration);
        jadConfig.setRepositories(Arrays.asList(
                new EnvironmentRepository(ENVIRONMENT_PREFIX),
                new SystemPropertiesRepository(PROPERTIES_PREFIX),
                new PropertiesRepository(configFile)
        ));

        LOG.debug("Loading configuration from config file: {}", configFile);
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        if (configuration.getRestTransportUri() == null) {
            configuration.setRestTransportUri(configuration.getDefaultRestTransportUri().toString());
            LOG.info("No rest_transport_uri set. Falling back to [{}].", configuration.getRestTransportUri());
        }

        return configuration;
    }
}
