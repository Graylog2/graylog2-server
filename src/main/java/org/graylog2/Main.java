/**
 * Copyright 2010, 2011 Lennart Koopmann <lennart@socketfeed.com>
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
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.graylog2.database.MongoConnection;
import org.graylog2.forwarders.forwarders.LogglyForwarder;
import org.graylog2.messagehandlers.amqp.AMQPBroker;
import org.graylog2.messagehandlers.amqp.AMQPSubscribedQueue;
import org.graylog2.messagehandlers.amqp.AMQPSubscriberThread;
import org.graylog2.messagehandlers.gelf.GELFMainThread;
import org.graylog2.messagehandlers.syslog.SyslogServerThread;
import org.graylog2.periodical.ChunkedGELFClientManagerThread;
import org.graylog2.periodical.HostCounterCacheWriterThread;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Properties;
import org.graylog2.periodical.MessageCountWriterThread;

/**
 * Main class of Graylog2.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);
    private static final String GRAYLOG2_VERSION = "0.9.5";
    
    public static RulesEngine drools = null;
    
    private Main() { }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        CommandLineArguments commandLineArguments = new CommandLineArguments();
        JCommander jCommander = new JCommander(commandLineArguments, args);
        jCommander.setProgramName("graylog2");

        if(commandLineArguments.isShowHelp()) {
            jCommander.usage();
            System.exit(0);
        }

        if(commandLineArguments.isShowVersion()) {
            System.out.println("Graylog2 Server " + GRAYLOG2_VERSION);
            System.out.println("JRE: " + Tools.getSystemInformation());
            System.exit(0);
        }

        // Are we in debug mode?
        if (commandLineArguments.isDebug()) {
            LOG.info("Running in Debug mode");
            Logger.getRootLogger().setLevel(Level.ALL);
            Logger.getLogger(Main.class.getPackage().getName()).setLevel(Level.ALL);
        }

        LOG.info("Graylog2 starting up. (JRE: " + Tools.getSystemInformation() + ")");

        String configFile = commandLineArguments.getConfigFile();
        LOG.info("Using config file: " + configFile);

        Configuration configuration = new Configuration(loadProperties(configFile));

        LOG.info("Checking configuration");
        try {
            configuration.validate();
        } catch (Exception e) {
            LOG.fatal("Invalid configuration: " + e.getMessage(), e);
            System.exit(1);
        }

        // If we only want to check our configuration, we can gracefully exit here
        if(commandLineArguments.isConfigTest()) {
            System.exit(0);
        }

        savePidFile(commandLineArguments.getPidFile());

        // Statically set timeout for LogglyForwarder.
        // TODO: This is a code smell and needs to be fixed.
        LogglyForwarder.setTimeout(configuration.getInteger("forwarder_loggly_timeout", 3));

        initializeMongoConnection(configuration);
        initializeRulesEngine(configuration.get("rules_file"));
        initializeSyslogServer(configuration.get("syslog_protocol"), configuration.getInteger("syslog_listen_port", 514));
        initializeHostCounterCache();

        // Start message counter thread.
        initializeMessageCounters();

        // Start GELF threads
        if (configuration.getBoolean("use_gelf")) {
            initializeGELFThreads(configuration.getInteger("gelf_listen_port", 12201));
        }

        // Initialize AMQP Broker if enabled
        if (configuration.getBoolean("amqp_enabled")) {
             initializeAMQP(configuration);
         }

        LOG.info("Graylog2 up and running.");
    }

    private static Properties loadProperties(String configFile) {
        Reader configFileReader = null;
        Properties properties = new Properties();

        try {
            configFileReader = new FileReader(configFile);
            properties.load(configFileReader);
        } catch(java.io.IOException e) {
            LOG.error("Could not read configuration file: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(configFileReader);
        }

        return properties;
    }

    private static void initializeHostCounterCache() {
        HostCounterCacheWriterThread hostCounterCacheWriterThread = new HostCounterCacheWriterThread();
        hostCounterCacheWriterThread.start();
        LOG.info("Host count cache is up.");
    }

    private static void initializeMessageCounters() {
        MessageCountWriterThread messageCountWriterThread = new MessageCountWriterThread();
        messageCountWriterThread.start();
        LOG.info("Message counters initialized.");
    }

    private static void initializeGELFThreads(int gelfPort) {
        GELFMainThread gelfThread = new GELFMainThread(gelfPort);
        gelfThread.start();

        ChunkedGELFClientManagerThread gelfManager = new ChunkedGELFClientManagerThread();
        gelfManager.start();

        LOG.info("GELF threads started");
    }

    private static void initializeSyslogServer(String syslogProtocol, int syslogPort) {

        // Start the Syslog thread that accepts syslog packages.
        SyslogServerThread syslogServerThread = new SyslogServerThread(syslogProtocol, syslogPort);
        syslogServerThread.start();

        // Check if the thread started up completely.
        try { Thread.sleep(1000); } catch(InterruptedException e) {}
        if(syslogServerThread.getCoreThread().isAlive()) {
            LOG.info("Syslog server thread is up.");
        } else {
            LOG.fatal("Could not start syslog server core thread. Do you have permissions to listen on port " + syslogPort + "?");
            System.exit(1);
        }
    }

    private static void initializeRulesEngine(String rulesFilePath) {
        try {
            if (rulesFilePath != null && !rulesFilePath.isEmpty()) {
                drools = new RulesEngine();
                drools.addRules(rulesFilePath);
                LOG.info("Using rules: " + rulesFilePath);
            } else {
                LOG.info("Not using rules");
            }
        } catch (Exception e) {
            LOG.fatal("Could not load rules engine: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void initializeMongoConnection(Configuration configuration) {
        try {
            MongoConnection.getInstance().connect(
                    configuration.get("mongodb_user"),
                    configuration.get("mongodb_password"),
                    configuration.get("mongodb_host"),
                    configuration.get("mongodb_database"),
                    configuration.getInteger("mongodb_port", 0),
                    configuration.get("mongodb_useauth"),
                    configuration.getMaximumMongoDBConnections(),
                    configuration.getThreadsAllowedToBlockMultiplier(),
                    configuration.getMongoDBReplicaSetServers(),
                    configuration.getLong("messages_collection_size", 50000000)
            );
        } catch (Exception e) {
            LOG.fatal("Could not create MongoDB connection: " + e.getMessage(), e);
            System.exit(1); // Exit with error.
        }
    }

    private static void initializeAMQP(Configuration configuration) {

        // Connect to AMQP broker.
        AMQPBroker amqpBroker = new AMQPBroker(
                configuration.get("amqp_host"),
                configuration.getInteger("amqp_port", 0),
                configuration.get("amqp_username"),
                configuration.get("amqp_password"),
                configuration.get("amqp_virtualhost")
        );

        List<AMQPSubscribedQueue> amqpQueues = configuration.getAMQPSubscribedQueues();

        if (amqpQueues != null) {
            // Start AMQP subscriber thread for each queue to listen on.
            for (AMQPSubscribedQueue queue : amqpQueues) {
                AMQPSubscriberThread amqpThread = new AMQPSubscriberThread(queue, amqpBroker);
                amqpThread.start();
            }

            LOG.info("AMQP threads started. (" + amqpQueues.size() + " queues)");
        }
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
        }
    }
}