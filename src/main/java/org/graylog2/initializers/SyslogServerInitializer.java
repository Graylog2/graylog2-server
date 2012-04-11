/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.initializers;

import org.apache.log4j.Logger;
import org.graylog2.Configuration;
import org.graylog2.GraylogServer;

/**
 * SyslogServerInitializer.java: Apr 11, 2012 5:06:27 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogServerInitializer implements Initializer {

    private static final Logger LOG = Logger.getLogger(SyslogServerInitializer.class);

    private final GraylogServer graylogServer;
    private final Configuration configuration;

    public SyslogServerInitializer(GraylogServer server, Configuration configuration) {
        this.graylogServer = server;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        /*// Start the Syslog thread that accepts syslog packages.
        SyslogServerThread syslogServerThread = new SyslogServerThread(
                graylogServer,
                configuration.getSyslogProtocol(),
                configuration.getSyslogListenPort(),
                configuration.getSyslogListenAddress()
        );
        
        syslogServerThread.start();

        // Check if the thread started up completely.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        if (syslogServerThread.getCoreThread().isAlive()) {
            LOG.info("Syslog server thread is up.");
        } else {
            LOG.fatal("Could not start syslog server core thread. Do you have permissions to listen on port " + configuration.getSyslogListenPort() + "?");
            System.exit(1);
        }*/
    }

}