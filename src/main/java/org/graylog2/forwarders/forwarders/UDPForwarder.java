/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.forwarders.forwarders;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

/**
 *
 * @author XING\lennart.koopmann
 */
public class UDPForwarder {

    private static final Logger LOG = Logger.getLogger(UDPForwarder.class);

    protected String host = null;
    protected int port = 0;

    protected boolean send(byte[] what) {
        DatagramSocket sock = null;

        try {
            sock = new DatagramSocket();
            InetAddress destination = InetAddress.getByName(this.getHost());

            DatagramPacket pkg = new DatagramPacket(what, what.length, destination, this.getPort());
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
