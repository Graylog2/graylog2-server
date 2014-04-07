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
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.graylog2.bindings.PersistenceServicesBindings;
import org.graylog2.bindings.ServerBindings;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.filters.*;
import org.graylog2.initializers.Initializers;
import org.graylog2.initializers.PeriodicalsInitializer;
import org.graylog2.inputs.ServerInputRegistry;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.inputs.gelf.tcp.GELFTCPInput;
import org.graylog2.inputs.gelf.udp.GELFUDPInput;
import org.graylog2.inputs.kafka.KafkaInput;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.misc.metrics.LocalMetricsInput;
import org.graylog2.inputs.radio.RadioInput;
import org.graylog2.inputs.random.FakeHttpMessageInput;
import org.graylog2.inputs.raw.tcp.RawTCPInput;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.inputs.syslog.tcp.SyslogTCPInput;
import org.graylog2.inputs.syslog.udp.SyslogUDPInput;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.outputs.ElasticSearchOutput;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugins.PluginInstaller;
import org.graylog2.shared.NodeRunner;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.graylog2.shared.filters.FilterRegistry;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

/**
 * Main class of Graylog2.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Main extends NodeRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

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
            System.out.println("Graylog2 Server " + Core.GRAYLOG2_VERSION);
            System.out.println("JRE: " + Tools.getSystemInformation());
            System.exit(0);
        }
        
        String configFile = commandLineArguments.getConfigFile();
        LOG.info("Using config file: {}", configFile);

        final Configuration configuration = getConfiguration(configFile);

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

        GuiceInstantiationService instantiationService = new GuiceInstantiationService();
        List<Module> bindingsModules = getBindingsModules(instantiationService,
                new ServerBindings(configuration),
                new PersistenceServicesBindings());
        Injector injector = Guice.createInjector(bindingsModules);
        instantiationService.setInjector(injector);

        // This is holding all our metrics.
        final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);

        // Report metrics via JMX.
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        InstrumentedAppender logMetrics = new InstrumentedAppender(metrics);
        logMetrics.activateOptions();
        org.apache.log4j.Logger.getRootLogger().setLevel(logLevel);
        org.apache.log4j.Logger.getLogger(Main.class.getPackage().getName()).setLevel(logLevel);
        org.apache.log4j.Logger.getRootLogger().addAppender(logMetrics);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        LOG.info("Graylog2 {} starting up. (JRE: {})", Core.GRAYLOG2_VERSION, Tools.getSystemInformation());

        // Do not use a PID file if the user requested not to
        if (!commandLineArguments.isNoPidFile()) {
            savePidFile(commandLineArguments.getPidFile());
        }

        // Le server object. This is where all the magic happens.
        Core server = injector.getInstance(Core.class);
        ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        serverStatus.setLifecycle(Lifecycle.STARTING);

        server.initialize();

        // Register this node.
        final NodeService nodeService = injector.getInstance(NodeService.class);
        nodeService.registerServer(serverStatus.getNodeId().toString(), configuration.isMaster(), configuration.getRestTransportUri());

        Node thisNode = null;
        try {
            thisNode = nodeService.byNodeId(serverStatus.getNodeId());
        } catch (NodeNotFoundException e) {
            throw new RuntimeException("Did not find own node. This should never happen.", e);
        }

        final ActivityWriter activityWriter = injector.getInstance(ActivityWriter.class);
        if (configuration.isMaster() && !nodeService.isOnlyMaster(serverStatus.getNodeId())) {
            LOG.warn("Detected another master in the cluster. Retrying in {} seconds to make sure it is not "
                    + "an old stale instance.", NodeServiceImpl.PING_TIMEOUT);
            try {
                Thread.sleep(NodeServiceImpl.PING_TIMEOUT*1000);
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
                        .addType(NotificationImpl.Type.MULTI_MASTER)
                        .addSeverity(NotificationImpl.Severity.URGENT);
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

        // Register standard inputs.
        InputRegistry inputRegistry = injector.getInstance(ServerInputRegistry.class);
        inputRegistry.register(SyslogUDPInput.class, SyslogUDPInput.NAME);
        inputRegistry.register(SyslogTCPInput.class, SyslogTCPInput.NAME);
        inputRegistry.register(RawUDPInput.class, RawUDPInput.NAME);
        inputRegistry.register(RawTCPInput.class, RawTCPInput.NAME);
        inputRegistry.register(GELFUDPInput.class, GELFUDPInput.NAME);
        inputRegistry.register(GELFTCPInput.class, GELFTCPInput.NAME);
        inputRegistry.register(GELFHttpInput.class, GELFHttpInput.NAME);
        inputRegistry.register(FakeHttpMessageInput.class, FakeHttpMessageInput.NAME);
        inputRegistry.register(LocalMetricsInput.class, LocalMetricsInput.NAME);
        inputRegistry.register(JsonPathInput.class, JsonPathInput.NAME);
        inputRegistry.register(KafkaInput.class, KafkaInput.NAME);
        inputRegistry.register(RadioInput.class, RadioInput.NAME);

        // Register initializers.
        Initializers initializers = injector.getInstance(Initializers.class);
        initializers.register(injector.getInstance(PeriodicalsInitializer.class));

        // Register message filters. (Order is important here)
        final FilterRegistry filterRegistry = injector.getInstance(FilterRegistry.class);
        filterRegistry.register(injector.getInstance(StaticFieldFilter.class));
        filterRegistry.register(injector.getInstance(ExtractorFilter.class));
        filterRegistry.register(injector.getInstance(BlacklistFilter.class));
        filterRegistry.register(injector.getInstance(StreamMatcherFilter.class));
        filterRegistry.register(injector.getInstance(RewriteFilter.class));

        // Register outputs.
        final OutputRegistry outputRegistry = injector.getInstance(OutputRegistry.class);
        outputRegistry.register(injector.getInstance(ElasticSearchOutput.class));

        // Start services.
        server.run();

        // Start REST API.
        try {
            server.startRestApi(injector);
        } catch(Exception e) {
            LOG.error("Could not start REST API on <{}>. Terminating.", configuration.getRestListenUri(), e);
            System.exit(1);
        }

        serverStatus.setLifecycle(Lifecycle.RUNNING);

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

    private static Configuration getConfiguration(String configFile) {
        final Configuration configuration = new Configuration();
        JadConfig jadConfig = new JadConfig(new PropertiesRepository(configFile), configuration);

        LOG.info("Loading configuration");
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration file: [{}]", configFile, e);
            System.exit(1);
        } catch (ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        return configuration;
    }

    private static void savePidFile(String pidFile) {

        String pid = Tools.getPID();
        Writer pidFileWriter = null;

        try {
            if (pid == null || pid.isEmpty() || pid.equals("unknown")) {
                throw new Exception("Could not determine PID.");
            }

            pidFileWriter = new FileWriter(pidFile);
            IOUtils.write(pid, pidFileWriter);
        } catch (Exception e) {
            LOG.error("Could not write PID file: " + e.getMessage(), e);
            System.exit(1);
        } finally {
            IOUtils.closeQuietly(pidFileWriter);
            // make sure to remove our pid when we exit
            new File(pidFile).deleteOnExit();
        }
    }

}
