/**
 * Copyright 2010, 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */

package org.graylog2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.log4j.Level;
import org.graylog2.activities.Activity;
import org.graylog2.alarms.transports.EmailTransport;
import org.graylog2.alarms.transports.JabberTransport;
import org.graylog2.filters.BlacklistFilter;
import org.graylog2.filters.CounterUpdateFilter;
import org.graylog2.filters.RewriteFilter;
import org.graylog2.filters.StreamMatcherFilter;
import org.graylog2.filters.TokenizerFilter;
import org.graylog2.initializers.AMQPSyncInitializer;
import org.graylog2.initializers.AlarmScannerInitializer;
import org.graylog2.initializers.AnonymousInformationCollectorInitializer;
import org.graylog2.initializers.BufferWatermarkInitializer;
import org.graylog2.initializers.DeflectorThreadsInitializer;
import org.graylog2.initializers.DroolsInitializer;
import org.graylog2.initializers.GraphiteInitializer;
import org.graylog2.initializers.HostCounterCacheWriterInitializer;
import org.graylog2.initializers.IndexRetentionInitializer;
import org.graylog2.initializers.LibratoMetricsInitializer;
import org.graylog2.initializers.MessageCounterInitializer;
import org.graylog2.initializers.ServerValueWriterInitializer;
import org.graylog2.initializers.StatisticsPrinterInitializer;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.gelf.GELFTCPInput;
import org.graylog2.inputs.gelf.GELFUDPInput;
import org.graylog2.inputs.http.GELFHttpInput;
import org.graylog2.inputs.syslog.SyslogTCPInput;
import org.graylog2.inputs.syslog.SyslogUDPInput;
import org.graylog2.plugin.Tools;
import org.graylog2.outputs.ElasticSearchOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;

/**
 * Wrapper for use with Apache Commons Daemon.
 *
 * @author $Author: kbrockhoff $
 * @version $Revision: 201129 $, $Date: 2013-01-03 08:26:44 -0600 (Thu, 03 Jan 2013) $
 */
public class ServerDaemon implements Daemon {

    private static final String[] EMPTY_ARGS = {};
    private static final Logger LOG = LoggerFactory.getLogger(ServerDaemon.class);
    
    private String[] args = EMPTY_ARGS;
    private Core server;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
        
    public ServerDaemon() {
        super();
    }

    @Override
    public void init(final DaemonContext context) throws DaemonInitException {
        args = context.getArguments();
    }

    @Override
    public void start() throws RepositoryException, ValidationException {
        
        final CommandLineArguments commandLineArguments = new CommandLineArguments();
        final JCommander jCommander = new JCommander(commandLineArguments, args);
        jCommander.setProgramName("graylog2");

        // Are we in debug mode?
        if (commandLineArguments.isDebug()) {
            LOG.info("Running in Debug mode");
            org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);
            org.apache.log4j.Logger.getLogger(Main.class.getPackage().getName()).setLevel(Level.ALL);
        }

        LOG.info("Graylog2 {} starting up. (JRE: {})", Core.GRAYLOG2_VERSION, Tools.getSystemInformation());

        String configFile = commandLineArguments.getConfigFile();
        LOG.info("Using config file: {}", configFile);

        final Configuration configuration = new Configuration();
        JadConfig jadConfig = new JadConfig(new PropertiesRepository(configFile), configuration);

        LOG.info("Loading configuration");
        jadConfig.process();

        // Le server object. This is where all the magic happens.
        server = new Core();
        server.initialize(configuration);
        
        // Could it be that there is another master instance already?
        if (configuration.isMaster() && server.cluster().masterCountExcept(server.getServerId()) != 0) {
            // All devils here.
            String what = "Detected other master node in the cluster! Starting as non-master! "
                    + "This is a mis-configuration you should fix.";
            LOG.warn(what);
            server.getActivityWriter().write(new Activity(what, Main.class));
            
            configuration.setIsMaster(false);
        }
        
        // Enable local mode?
        if (commandLineArguments.isLocal() || commandLineArguments.isDebug()) {
            // In local mode, systemstats are sent to localhost for example.
            LOG.info("Running in local mode");
            server.setLocalMode(true);
        }

        // Are we in stats mode?
        if (commandLineArguments.isStats()) {
            LOG.info("Printing system utilization information.");
            server.setStatsMode(true);
        }
        
        // Register transports.
        if (configuration.isTransportEmailEnabled()) { server.registerTransport(new EmailTransport()); }
        if (configuration.isTransportJabberEnabled()) {  server.registerTransport(new JabberTransport()); }

        // Register initializers.
        server.registerInitializer(new ServerValueWriterInitializer());
        server.registerInitializer(new DroolsInitializer());
        server.registerInitializer(new HostCounterCacheWriterInitializer());
        server.registerInitializer(new MessageCounterInitializer());
        server.registerInitializer(new AlarmScannerInitializer());
        if (configuration.isEnableGraphiteOutput())       { server.registerInitializer(new GraphiteInitializer()); }
        if (configuration.isEnableLibratoMetricsOutput()) { server.registerInitializer(new LibratoMetricsInitializer()); }
        server.registerInitializer(new DeflectorThreadsInitializer());
        server.registerInitializer(new AnonymousInformationCollectorInitializer());
        if (configuration.performRetention() && commandLineArguments.performRetention()) {
            server.registerInitializer(new IndexRetentionInitializer());
        }
        if (configuration.isAmqpEnabled()) {
            server.registerInitializer(new AMQPSyncInitializer());
        }
        server.registerInitializer(new BufferWatermarkInitializer());
        if (commandLineArguments.isStats()) { server.registerInitializer(new StatisticsPrinterInitializer()); }
        
        // Register inputs.
        if (configuration.isUseGELF()) {
            server.registerInput(new GELFUDPInput());
            server.registerInput(new GELFTCPInput());
        }
        
        if (configuration.isSyslogUdpEnabled()) { server.registerInput(new SyslogUDPInput()); }
        if (configuration.isSyslogTcpEnabled()) { server.registerInput(new SyslogTCPInput()); }

        if (configuration.isAmqpEnabled()) { server.registerInput(new AMQPInput()); }

        if (configuration.isHttpEnabled()) { server.registerInput(new GELFHttpInput()); }

        // Register message filters.
        server.registerFilter(new RewriteFilter());
        server.registerFilter(new BlacklistFilter());
        if (configuration.isEnableTokenizerFilter()) { server.registerFilter(new TokenizerFilter()); }
        server.registerFilter(new StreamMatcherFilter());
        server.registerFilter(new CounterUpdateFilter());

        // Register outputs.
        server.registerOutput(new ElasticSearchOutput());
        
        // initialize the components
        server.initializeComponents();
        
        // Set running
        executorService.execute(server);
    }

    @Override
    public void stop() {
        if (server != null) {
            LOG.info("Graylog2 {} exiting.", Core.GRAYLOG2_VERSION);
            server.setProcessing(false);
            executorService.shutdown();
        }
    }

    @Override
    public void destroy() {
        if (!executorService.isShutdown()) {
            stop();
        }
        server = null;
    }

}
