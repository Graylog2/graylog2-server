/**
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
import com.github.joschi.jadconfig.jodatime.JodaTimeConverterFactory;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;
import com.mongodb.MongoException;
import org.apache.log4j.Level;
import org.graylog2.bindings.AlarmCallbackBindings;
import org.graylog2.bindings.InitializerBindings;
import org.graylog2.bindings.MessageFilterBindings;
import org.graylog2.bindings.MessageOutputBindings;
import org.graylog2.bindings.PersistenceServicesBindings;
import org.graylog2.bindings.RotationStrategyBindings;
import org.graylog2.bindings.ServerBindings;
import org.graylog2.bindings.ServerMessageInputBindings;
import org.graylog2.cluster.NodeService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.NodeRunner;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.graylog2.shared.plugins.PluginLoader;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Main class of Graylog2.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Main extends NodeRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String ENVIRONMENT_PREFIX = "GRAYLOG2_";
    private static final String PROPERTIES_PREFIX = "graylog2.";

    private static final String profileName = "Server";

    private static final Version version = Version.CURRENT_CLASSPATH;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // So jung kommen wir nicht mehr zusammen.

        final CommandLineArguments commandLineArguments = new CommandLineArguments();
        final JCommander jCommander = new JCommander(commandLineArguments, args);
        jCommander.setProgramName("graylog2-" + profileName.toLowerCase());

        if (commandLineArguments.isShowHelp()) {
            jCommander.usage();
            System.exit(0);
        }

        if (commandLineArguments.isShowVersion()) {
            System.out.println("Graylog2 " + profileName + " " + version);
            System.out.println("JRE: " + Tools.getSystemInformation());
            System.exit(0);
        }

        if(commandLineArguments.isDumpDefaultConfig()) {
            final JadConfig jadConfig = new JadConfig();
            jadConfig.addConverterFactory(new JodaTimeConverterFactory());
            jadConfig.addConfigurationBean(new Configuration());
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        final JadConfig jadConfig = new JadConfig();
        jadConfig.addConverterFactory(new JodaTimeConverterFactory());
        final Configuration configuration = readConfiguration(jadConfig, commandLineArguments.getConfigFile());

        if(commandLineArguments.isDumpConfig()) {
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        if (configuration.getPasswordSecret().isEmpty()) {
            LOG.error("No password secret set. Please define password_secret in your graylog2.conf.");
            System.exit(1);
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

        final Injector injector = setupInjector(configuration, pluginModules);

        if (injector == null) {
            LOG.error("Injector could not be created, exiting! (Please include the previous stacktraces in bug reports.)");
            System.exit(1);
        }

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

        LOG.info("Graylog2 " + profileName + " {} starting up. (JRE: {})", version, Tools.getSystemInformation());

        // Do not use a PID file if the user requested not to
        if (!commandLineArguments.isNoPidFile()) {
            savePidFile(commandLineArguments.getPidFile());
        }

        // Le server object. This is where all the magic happens.
        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        serverStatus.initialize();

        final ActivityWriter activityWriter;
        final ServiceManager serviceManager;
        try {
            activityWriter = injector.getInstance(ActivityWriter.class);
            serviceManager = injector.getInstance(ServiceManager.class);
        } catch (ProvisionException e) {
            for (Message message : e.getErrorMessages()) {
                if (message.getCause() instanceof MongoException) {
                    LOG.error(UI.wallString("Unable to connect to MongoDB. Is it running and the configuration correct?"));
                    System.exit(-1);
                }
            }

            LOG.error("Guice error", e);
            System.exit(-1);
            return;
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            System.exit(-1);
            return;
        }

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
        serviceManager.addListener(serviceManagerListener);
        try {
            serviceManager.startAsync().awaitHealthy();
        } catch (Exception e) {
            try {
                serviceManager.stopAsync().awaitStopped(configuration.getShutdownTimeout(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                LOG.error("Unable to shutdown properly on time. {}", serviceManager.servicesByState());
            }
            LOG.error("Graylog2 startup failed. Exiting. Exception was:", e);
            System.exit(-1);
        }
        LOG.info("Services started, startup times in ms: {}", serviceManager.startupTimes());

        activityWriter.write(new Activity("Started up.", Main.class));
        LOG.info("Graylog2 " + profileName + " up and running.");

        // Block forever.
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            return;
        }
    }

    private static Injector setupInjector(Configuration configuration, List<PluginModule> pluginModules) {
        try {
            GuiceInstantiationService instantiationService = new GuiceInstantiationService();
            List<Module> bindingsModules = getBindingsModules(instantiationService,
                    new ServerBindings(configuration),
                    new PersistenceServicesBindings(),
                    new ServerMessageInputBindings(),
                    new MessageFilterBindings(),
                    new AlarmCallbackBindings(),
                    new InitializerBindings(),
                    new MessageOutputBindings(),
                    new RotationStrategyBindings());
            LOG.debug("Adding plugin modules: " + pluginModules);
            bindingsModules.addAll(pluginModules);
            final Injector injector = GuiceInjectorHolder.createInjector(bindingsModules);
            instantiationService.setInjector(injector);

            return injector;
        } catch (Exception e) {
            LOG.error("Injector creation failed!", e);
            return null;
        }
    }

    private static String dumpConfiguration(final Map<String, String> configMap) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Configuration of graylog2-").append(profileName).append(" ").append(version).append(System.lineSeparator());
        sb.append("# Generated on ").append(Tools.iso8601()).append(System.lineSeparator());

        for(Map.Entry<String, String> entry:  configMap.entrySet()) {
            sb.append(entry.getKey()).append('=').append(nullToEmpty(entry.getValue())).append(System.lineSeparator());
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
            configuration.setRestTransportUri(configuration.getDefaultRestTransportUri());
            LOG.debug("No rest_transport_uri set. Using default [{}].", configuration.getRestTransportUri());
        }

        return configuration;
    }
}
