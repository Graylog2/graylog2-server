/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
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

package graylog2;


import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author Lennart Koopmann <lennart@scopeport.org>
 */
public class Main {

    public static boolean debugMode = false;

    // This holds the configuration from /etc/graylog2.conf
    public static Properties masterConfig = null;

    public static Thread syslogCoreThread = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("[x] Graylog2 starting up. (JRE: " + Tools.getSystemInformation() + ")");

        // Read config.
        System.out.println("[x] Reading config.");
        Main.masterConfig = new Properties();
        try {
            FileInputStream configStream = new FileInputStream("/etc/graylog2.conf");
            Main.masterConfig.load(configStream);
            configStream.close();
        } catch(java.io.IOException e) {
            System.out.println("Could not read config file: " + e.toString());
        }

        // Define required configuration fields.
        ArrayList requiredConfigFields = new ArrayList();
        requiredConfigFields.add("syslog_listen_port");
        requiredConfigFields.add("mongodb_user");
        requiredConfigFields.add("mongodb_password");
        requiredConfigFields.add("mongodb_host");
        requiredConfigFields.add("mongodb_database");
        requiredConfigFields.add("mongodb_port");

        // Check if all required configuration fields are set.
        for (Object requiredConfigFieldO : requiredConfigFields) {
            String requiredConfigField = (String) requiredConfigFieldO;
            try {
                if (Main.masterConfig.getProperty(requiredConfigField).length() <= 0) {
                    throw new Exception("Not set");
                }
            } catch (Exception e) {
                System.out.println("Missing configuration variable '" + requiredConfigField + "' - Terminating. (" + e.toString() + ")");
                System.exit(1); // Exit with error.
            }
        }

        // Are we in debug mode?
        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            System.out.println("[x] Running in Debug mode");
            Main.debugMode = true;
        } else {
            System.out.println("[x] Not in Debug mode.");
        }

        // Start the Syslog thread that accepts syslog packages.
        SyslogServerThread syslogServerThread = new SyslogServerThread(Integer.parseInt(Main.masterConfig.getProperty("syslog_listen_port")));
        syslogServerThread.start();

        // Check if the thread started up completely.
        try { Thread.sleep(1000); } catch(InterruptedException e) {}
        if(!syslogCoreThread.isAlive()) {
            System.out.println("Could not start syslog server core thread. Do you have permissions to listen on UDP port " + Main.masterConfig.getProperty("syslog_listen_port") + "?");
            System.exit(1); // Exit with error.
        }

        // Start the thread that continously collects system information.
        SystemStatisticThread systemStatisticThread = new SystemStatisticThread();
        systemStatisticThread.start();
        System.out.println("[x] System statistic thread is up.");

        System.out.println("[x] Syslog server up and running.");
    }

}
