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

import org.apache.log4j.Logger;
import org.graylog2.database.MongoConnection;
import org.graylog2.messagehandlers.amqp.AMQP;
import org.graylog2.messagehandlers.amqp.AMQPBroker;
import org.graylog2.messagehandlers.amqp.AMQPSubscribedQueue;
import org.graylog2.messagehandlers.amqp.AMQPSubscriberThread;
import org.graylog2.messagehandlers.gelf.GELF;
import org.graylog2.messagehandlers.gelf.GELFMainThread;
import org.graylog2.messagehandlers.syslog.SyslogServerThread;
import org.graylog2.periodical.ChunkedGELFClientManagerThread;
import org.graylog2.periodical.HostCounterCacheWriterThread;
import org.graylog2.periodical.ServerValueWriterThread;
import org.graylog2.periodical.ThroughputWriterThread;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;

/**
 * Main class of Graylog2.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    /**
     * This holds the configuration from /etc/graylog2.conf
     */
    public static Properties masterConfig = null;
    
    /**
     * This holds the filter out regular expressions. Defined in masterConfig
     */
    public static final String GRAYLOG2_VERSION = "0.9.5";
    
    public static RulesEngine drools = null;
    
    private Main() { }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Logger.getRootLogger().addAppender(new SelfLogAppender());

        // Are we in debug mode?
        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            LOG.info("[x] Running in Debug mode");
            Logger.getRootLogger().setLevel(Level.ALL);
            Logger.getLogger("org.graylog2").setLevel(Level.ALL);
        }

        LOG.info("[x] Graylog2 starting up. (JRE: " + Tools.getSystemInformation() + ")");

        // Read config.
        LOG.info("[x] Reading config.");
        Main.masterConfig = new Properties();
        // Allow -DconfigPath=/some/different/config.
        String configPath = System.getProperty("configPath", "/etc/graylog2.conf");
        LOG.info("[x] Using config: " + configPath);

        try {
            FileInputStream configStream = new FileInputStream(configPath);
            Main.masterConfig.load(configStream);
            configStream.close();
        } catch(java.io.IOException e) {
            LOG.error("Could not read config file: " + e.getMessage(), e);
        }
        
        // Define required configuration fields.
        List<String> requiredConfigFields = new ArrayList<String>();
        requiredConfigFields.add("syslog_listen_port");
        requiredConfigFields.add("syslog_protocol");
        requiredConfigFields.add("mongodb_useauth");
        requiredConfigFields.add("mongodb_user");
        requiredConfigFields.add("mongodb_password");
        requiredConfigFields.add("mongodb_database");
        requiredConfigFields.add("mongodb_port");
        requiredConfigFields.add("messages_collection_size");
        requiredConfigFields.add("use_gelf");
        requiredConfigFields.add("gelf_listen_port");

        // Check if all required configuration fields are set.
        for (String requiredConfigField : requiredConfigFields) {
            try {
                if (Main.masterConfig.getProperty(requiredConfigField).length() <= 0) {
                    throw new Exception("Not set");
                }
            } catch (Exception e) {
                LOG.fatal("Missing configuration variable '" + requiredConfigField + "' - Terminating. (" + e.getMessage() + ")", e);
                System.exit(1); // Exit with error.
            }
        }

        // Check if a MongoDB replica set or host is defined.
        if (Main.masterConfig.getProperty("mongodb_host") == null && Main.masterConfig.getProperty("mongodb_replica_set") == null) {
            LOG.fatal("No MongoDB host (mongodb_host) or replica set (mongodb_replica_set) defined. Terminating.");
            System.exit(1); // Exit with error.
        }

        // Is the syslog_procotol valid? ("tcp"/"udp")
        List<String> allowedSyslogProtocols = new ArrayList<String>();
        allowedSyslogProtocols.add("tcp");
        allowedSyslogProtocols.add("udp");
        if(!allowedSyslogProtocols.contains(Main.masterConfig.getProperty("syslog_protocol"))) {
            LOG.fatal("Invalid syslog_protocol: " + Main.masterConfig.getProperty("syslog_protocol"));
            System.exit(1); // Exit with error.
        }

        // Print out a deprecation warning if "rrd_storage_dir" is set.
        if (Main.masterConfig.getProperty("rrd_storage_dir") != null) {
            LOG.warn("[!] Deprecation warning: Config parameter rrd_storage_dir is no longer needed.");
        }

        // Write a PID file.
        try {
            String pid = Tools.getPID();
            if (pid == null || pid.length() == 0 || pid.equals("unknown")) {
                throw new Exception("Could not determine PID.");
            }

            FileWriter fstream = new FileWriter("/tmp/graylog2.pid");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(pid);
            out.close();
        } catch (Exception e) {
            LOG.fatal("Could not write PID file: " + e.getMessage(), e);
            System.exit(1); // Exit with error.
        }

        try {
            MongoConnection.getInstance().connect(
                    Main.masterConfig.getProperty("mongodb_user"),
                    Main.masterConfig.getProperty("mongodb_password"),
                    Main.masterConfig.getProperty("mongodb_host"),
                    Main.masterConfig.getProperty("mongodb_database"),
                    (Main.masterConfig.getProperty("mongodb_port") == null) ? 0 : Integer.parseInt(Main.masterConfig.getProperty("mongodb_port")),
                    Main.masterConfig.getProperty("mongodb_useauth"),
                    Configuration.getMongoDBReplicaSetServers(Main.masterConfig)
            );
        } catch (Exception e) {
            LOG.fatal("Could not create MongoDB connection: " + e.getMessage(), e);
            System.exit(1); // Exit with error.
        }

        // Fill some stuff into the server_values collection.
        ServerValue.setStartupTime(Tools.getUTCTimestamp());
        ServerValue.setPID(Integer.parseInt(Tools.getPID()));
        ServerValue.setJREInfo(Tools.getSystemInformation());
        ServerValue.setGraylog2Version(GRAYLOG2_VERSION);
        ServerValue.setAvailableProcessors(HostSystem.getAvailableProcessors());
        ServerValue.setLocalHostname(Tools.getLocalHostname());

        // Create Rules Engine
        try {
            String rulesFilePath = Main.masterConfig.getProperty("rules_file");
            if (rulesFilePath != null && !rulesFilePath.isEmpty()) {
                Main.drools = new RulesEngine();
                Main.drools.addRules(rulesFilePath);
                LOG.info("[x] Using rules: " + rulesFilePath);
            } else {
                LOG.info("[x] Not using rules");
            }
        } catch (Exception e) {
            LOG.fatal("Could not load rules engine: " + e.getMessage(), e);
            System.exit(1); // Exit with error.
        }
		
        // Start the Syslog thread that accepts syslog packages.
        SyslogServerThread syslogServerThread = new SyslogServerThread(Integer.parseInt(Main.masterConfig.getProperty("syslog_listen_port")));
        syslogServerThread.start();

        // Check if the thread started up completely.
        try { Thread.sleep(1000); } catch(InterruptedException e) {}
        if(syslogServerThread.getCoreThread().isAlive()) {
            LOG.info("[x] Syslog server thread is up.");
        } else {
            LOG.fatal("Could not start syslog server core thread. Do you have permissions to listen on port " + Main.masterConfig.getProperty("syslog_listen_port") + "?");
            System.exit(1); // Exit with error.
        }

        // Start GELF threads.
        if (GELF.isEnabled()) {
            GELFMainThread gelfThread = new GELFMainThread(Integer.parseInt(Main.masterConfig.getProperty("gelf_listen_port")));
            gelfThread.start();

            ChunkedGELFClientManagerThread gelfManager = new ChunkedGELFClientManagerThread();
            gelfManager.start();
            
            LOG.info("[x] GELF threads are up.");
        }

        // Host counter cache.
        HostCounterCacheWriterThread hostCounterCacheWriterThread = new HostCounterCacheWriterThread();
        hostCounterCacheWriterThread.start();
        LOG.info("[x] Host count cache is up.");

        // AMQP.
         if (AMQP.isEnabled(Main.masterConfig)) {
            // Connect to AMQP broker.
            AMQPBroker amqpBroker = new AMQPBroker(
                    Main.masterConfig.getProperty("amqp_host"),
                    (Main.masterConfig.getProperty("amqp_port") == null) ? 0 : Integer.parseInt(Main.masterConfig.getProperty("amqp_port")),
                    Main.masterConfig.getProperty("amqp_username"),
                    Main.masterConfig.getProperty("amqp_password"),
                    Main.masterConfig.getProperty("amqp_virtualhost")
            );

            List<AMQPSubscribedQueue> amqpQueues = Configuration.getAMQPSubscribedQueues(Main.masterConfig);

            if (amqpQueues != null) {
                // Start AMQP subscriber thread for each queue to listen on.
                for (AMQPSubscribedQueue queue : amqpQueues) {
                    AMQPSubscriberThread amqpThread = new AMQPSubscriberThread(queue, amqpBroker);
                    amqpThread.start();
                }

                LOG.info("[x] AMQP threads are up. (" + amqpQueues.size() + " queues)");
            }
        }

        // Start thread that stores throughput info.
        ThroughputWriterThread throughputThread = new ThroughputWriterThread();
        throughputThread.start();

        // Start thread that stores system information periodically.
        ServerValueWriterThread serverValueThread = new ServerValueWriterThread();
        serverValueThread.start();

        LOG.info("[x] Graylog2 up and running.");
    }

}
