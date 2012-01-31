/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.syslog;

import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

/**
 * SyslogServerThread.java: May 17, 2010 9:23:33 PM
 *
 * Listen for Syslog messages
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogServerThread extends Thread {

    private int port;
    private String host;
    private String syslogProtocol;

    private Thread coreThread = null;

    /**
     * Listen for Syslog messages
     *
     * @param syslogProtocol Protocol to use for the Syslog server (tcp/udp)
     * @param port On which port to listen?
     */
    public SyslogServerThread(String syslogProtocol, int port, String host) {
        this.syslogProtocol = syslogProtocol;
        this.port = port;
        this.host = host;
    }

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {

        SyslogServerIF syslogServer = SyslogServer.getInstance(syslogProtocol);
        SyslogServerConfigIF syslogServerConfig = syslogServer.getConfig();
        
        syslogServerConfig.setPort(port);
        syslogServerConfig.setHost(host);
        syslogServerConfig.setUseStructuredData(true);
        syslogServerConfig.addEventHandler(new SyslogEventHandler());

        syslogServer = SyslogServer.getThreadedInstance(syslogProtocol);

        this.coreThread = syslogServer.getThread();
    }

    /**
     * Get the thread all syslog handling is running in
     * 
     * @return
     */
    public Thread getCoreThread() {
        return this.coreThread;
    }

}

