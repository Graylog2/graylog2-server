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

import com.beust.jcommander.JCommander;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.graylog2.activities.Activity;
import org.graylog2.communicator.methods.TwilioCommunicator;
import org.graylog2.filters.BlacklistFilter;
import org.graylog2.filters.CounterUpdateFilter;
import org.graylog2.filters.RewriteFilter;
import org.graylog2.filters.StreamMatcherFilter;
import org.graylog2.filters.TokenizerFilter;
import org.graylog2.initializers.*;
import org.graylog2.inputs.gelf.GELFTCPInput;
import org.graylog2.inputs.gelf.GELFUDPInput;
import org.graylog2.inputs.syslog.SyslogTCPInput;
import org.graylog2.inputs.syslog.SyslogUDPInput;
import org.graylog2.outputs.ElasticSearchOutput;

/**
 * Main class of Graylog2.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

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

        // Are we in debug mode?
        if (commandLineArguments.isDebug()) {
            LOG.info("Running in Debug mode");
            Logger.getRootLogger().setLevel(Level.ALL);
            Logger.getLogger(Main.class.getPackage().getName()).setLevel(Level.ALL);
        }

        LOG.info("Graylog2 " + Core.GRAYLOG2_VERSION + " starting up. (JRE: " + Tools.getSystemInformation() + ")");

        String configFile = commandLineArguments.getConfigFile();
        LOG.info("Using config file: " + configFile);

        final Configuration configuration = new Configuration();
        JadConfig jadConfig = new JadConfig(new PropertiesRepository(configFile), configuration);

        LOG.info("Loading configuration");
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.fatal("Couldn't load configuration file " + configFile, e);
            System.exit(1);
        } catch (ValidationException e) {
            LOG.fatal("Invalid configuration", e);
            System.exit(1);
        }

        // If we only want to check our configuration, we just initialize the rules engine to check if the rules compile
        if (commandLineArguments.isConfigTest()) {
            Core server = new Core();
            DroolsInitializer drools = new DroolsInitializer(server, configuration);
            drools.initialize();
            // rules have been checked, exit gracefully
            System.exit(0);
        }

        // Do not use a PID file if the user requested not to
        if (!commandLineArguments.isNoPidFile()) {
            savePidFile(commandLineArguments.getPidFile());
        }

        // Le server object. This is where all the magic happens.
        Core server = new Core();
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
        
        if (commandLineArguments.isLocal() || commandLineArguments.isDebug()) {
            // In local mode, systemstats are sent to localhost for example.
            LOG.info("Running in local mode");
            server.setLocalMode(true);
        }
        
        // Register communicator methods.
        if (configuration.isEnableCommunicationMethodTwilio()) {
            server.registerCommunicatorMethod(TwilioCommunicator.class);
        }
        
        // Register initializers.
        server.registerInitializer(new ServerValueWriterInitializer(server, configuration));
        server.registerInitializer(new DroolsInitializer(server, configuration));
        server.registerInitializer(new HostCounterCacheWriterInitializer(server));
        server.registerInitializer(new MessageCounterInitializer(server));
        if (configuration.isEnableGraphiteOutput())       { server.registerInitializer(new GraphiteInitializer(server)); }
        if (configuration.isEnableLibratoMetricsOutput()) { server.registerInitializer(new LibratoMetricsInitializer(server)); }
        server.registerInitializer(new DeflectorThreadsInitializer(server));
        server.registerInitializer(new AnonymousInformationCollectorInitializer(server));
        if (configuration.performRetention() && commandLineArguments.performRetention()) {
            server.registerInitializer(new IndexRetentionInitializer(server));
        }
        
        // Register inputs.
        if (configuration.isUseGELF()) {
            server.registerInput(new GELFUDPInput());
            server.registerInput(new GELFTCPInput());
        }
        
        if (configuration.isSyslogUdpEnabled()) { server.registerInput(new SyslogUDPInput()); }
        if (configuration.isSyslogTcpEnabled()) { server.registerInput(new SyslogTCPInput()); }

        // Register message filters.
        server.registerFilter(new RewriteFilter());
        server.registerFilter(new BlacklistFilter());
        if (configuration.isEnableTokenizerFilter()) { server.registerFilter(new TokenizerFilter()); }
        server.registerFilter(new StreamMatcherFilter());
        server.registerFilter(new CounterUpdateFilter());

        // Register outputs.
        server.registerOutput(ElasticSearchOutput.class);
        
        // Blocks until we shut down.
        server.run();

        LOG.info("Graylog2 " + Core.GRAYLOG2_VERSION + " exiting.");
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
            LOG.fatal("Could not write PID file: " + e.getMessage(), e);
            System.exit(1);
        } finally {
            IOUtils.closeQuietly(pidFileWriter);
            // make sure to remove our pid when we exit
            new File(pidFile).deleteOnExit();
        }
    }

}
