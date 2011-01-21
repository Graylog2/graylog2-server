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

import java.io.BufferedWriter;
import org.graylog2.messagehandlers.syslog.SyslogServerThread;
import org.graylog2.messagehandlers.gelf.GELFMainThread;
import org.graylog2.messagehandlers.gelf.GELF;
import org.graylog2.database.MongoConnection;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.graylog2.messagehandlers.amqp.AMQP;
import org.graylog2.messagehandlers.amqp.AMQPBroker;
import org.graylog2.messagehandlers.amqp.AMQPSubscribedQueue;
import org.graylog2.messagehandlers.amqp.AMQPSubscriberThread;
import org.graylog2.periodical.ChunkedGELFClientManagerThread;
import org.graylog2.periodical.ServerValueHistoryWriterThread;
import org.graylog2.periodical.ThroughputWriterThread;

/**
 * Main class of Graylog2.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Main {

    /**
     * Controlled by parameter "debug". Enables more verbose output.
     */
    public static boolean debugMode = false;

    /**
     * This holds the configuration from /etc/graylog2.conf
     */
    public static Properties masterConfig = null;

    public static final String GRAYLOG2_VERSION = "0.9.5-dev";
    

    private Main() { }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("[x] Graylog2 starting up. (JRE: " + Tools.getSystemInformation() + ")");

        // Read config.
        System.out.println("[x] Reading config.");
        Main.masterConfig = new Properties();
        // Allow -DconfigPath=/some/different/config.
        String configPath = System.getProperty("configPath", "/etc/graylog2.conf");
        System.out.println("[x] Using config: " + configPath);

        try {
            FileInputStream configStream = new FileInputStream(configPath);
            Main.masterConfig.load(configStream);
            configStream.close();
        } catch(java.io.IOException e) {
            System.out.println("Could not read config file: " + e.toString());
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
                System.out.println("Missing configuration variable '" + requiredConfigField + "' - Terminating. (" + e.toString() + ")");
                System.exit(1); // Exit with error.
            }
        }

        // Check if a MongoDB replica set or host is defined.
        if (Main.masterConfig.getProperty("mongodb_host") == null && Main.masterConfig.getProperty("mongodb_replica_set") == null) {
            System.out.println("No MongoDB host (mongodb_host) or replica set (mongodb_replica_set) defined. Terminating.");
            System.exit(1); // Exit with error.
        }

        // Is the syslog_procotol valid? ("tcp"/"udp")
        List<String> allowedSyslogProtocols = new ArrayList<String>();
        allowedSyslogProtocols.add("tcp");
        allowedSyslogProtocols.add("udp");
        if(!allowedSyslogProtocols.contains(Main.masterConfig.getProperty("syslog_protocol"))) {
            System.out.println("Invalid syslog_protocol: " + Main.masterConfig.getProperty("syslog_protocol"));
            System.exit(1); // Exit with error.
        }

        // Print out a deprecation warning if "rrd_storage_dir" is set.
        if (Main.masterConfig.getProperty("rrd_storage_dir") != null) {
            System.out.println("[!] Deprecation warning: Config parameter rrd_storage_dir is no longer needed.");
        }

        // Are we in debug mode?
        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            System.out.println("[x] Running in Debug mode");
            Main.debugMode = true;
        } else {
            System.out.println("[x] Not in Debug mode.");
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
            System.out.println("Could not write PID file: " + e.toString());
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
            System.out.println("Could not create MongoDB connection: " + e.toString());
            e.printStackTrace();
            System.exit(1); // Exit with error.
        }

        // Fill some stuff into the server_values collection.
        ServerValue.setStartupTime(Tools.getUTCTimestamp());
        ServerValue.setPID(Integer.parseInt(Tools.getPID()));
        ServerValue.setJREInfo(Tools.getSystemInformation());
        ServerValue.setGraylog2Version(GRAYLOG2_VERSION);
        ServerValue.setAvailableProcessors(HostSystem.getAvailableProcessors());
        ServerValue.setLocalHostname(Tools.getLocalHostname());

        // Start the Syslog thread that accepts syslog packages.
        SyslogServerThread syslogServerThread = new SyslogServerThread(Integer.parseInt(Main.masterConfig.getProperty("syslog_listen_port")));
        syslogServerThread.start();

        // Check if the thread started up completely.
        try { Thread.sleep(1000); } catch(InterruptedException e) {}
        if(syslogServerThread.getCoreThread().isAlive()) {
            System.out.println("[x] Syslog server thread is up.");
        } else {
            System.out.println("Could not start syslog server core thread. Do you have permissions to listen on port " + Main.masterConfig.getProperty("syslog_listen_port") + "?");
            System.exit(1); // Exit with error.
        }

        // Start GELF threads.
        if (GELF.isEnabled()) {
            GELFMainThread gelfThread = new GELFMainThread(Integer.parseInt(Main.masterConfig.getProperty("gelf_listen_port")));
            gelfThread.start();

            ChunkedGELFClientManagerThread gelfManager = new ChunkedGELFClientManagerThread();
            gelfManager.start();
            
            System.out.println("[x] GELF threads are up.");
        }

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

                System.out.println("[x] AMQP threads are up. (" + amqpQueues.size() + " queues)");
            }
        }

        // Start thread that stores throughput info.
        ThroughputWriterThread throughputThread = new ThroughputWriterThread();
        throughputThread.start();

        // Start thread that stores system information periodically.
        ServerValueHistoryWriterThread serverValueHistoryThread = new ServerValueHistoryWriterThread();
        serverValueHistoryThread.start();

        System.out.println("[x] Graylog2 up and running.");
    }

}
