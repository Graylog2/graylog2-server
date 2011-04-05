/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.forwarders.forwarders;

import org.apache.log4j.Logger;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.graylog2.forwarders.MessageForwarderIF;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * SyslogForwarder.java: Apr 5, 2011 8:23:14 PM
 *
 * Forwards syslog messages to other syslog endpoints.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class UDPSyslogForwarder implements MessageForwarderIF {

    private static final Logger LOG = Logger.getLogger(LogglyForwarder.class);

    private String host = null;
    private int port = 0;

    private boolean succeeded = false;

    public UDPSyslogForwarder(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean forward(GELFMessage message) throws MessageForwarderConfigurationException {
        if (this.host.isEmpty() || this.port <= 0) {
            throw new MessageForwarderConfigurationException("Host is empty or port is invalid.");
        }

        this.succeeded = this.send(message.getShortMessage());
        return this.succeeded();
    }

    private boolean send(String what) {
        DatagramSocket sock = null;

        try {
            sock = new DatagramSocket();
            InetAddress destination = InetAddress.getByName(this.getHost());

            DatagramPacket pkg = new DatagramPacket(what.getBytes(), what.getBytes().length, destination, this.getPort());
            sock.send(pkg);
        } catch (SocketException e) {
            LOG.error("Could not forward UDP syslog message: " + e.getMessage(), e);
        } catch (UnknownHostException e) {
            LOG.warn("Could not forward UDP syslog message (Unknown host: <" + this.getHost() + ">): " + e.getMessage(), e);
        } catch (IOException e) {
            LOG.error("Could not forward UDP syslog message (IO error): " + e.getMessage(), e);
        } finally {
            if (sock != null) {
                sock.close();
            }
        }

        return true;
    }

    /**
     * Indicates if the last forward has succeeded. This is not guaranteeing
     * delivery for the SyslogForwarder as it it sending UDP.
     *
     * @return
     */
    public boolean succeeded() {
        return this.succeeded;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

}
