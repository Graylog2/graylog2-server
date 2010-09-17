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

package org.graylog2.messagehandlers.gelf;

import org.graylog2.Log;

import java.net.*;
import java.io.*;

/**
 * GELFThread.java: Jun 23, 2010 6:58:07 PM
 *
 * Server that can listen for GELF messages.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFServer {

    /**
     * The maximum packet size. (8192 is reasonable UDP limit)
     */
    public static final int MAX_PACKET_SIZE = 8192;

    private DatagramSocket serverSocket = null;

    /**
     * Create the UDP socket.
     *
     * @param port The port to listen on.
     * @return boolean
     */
    public boolean create(int port) {
        try {
            this.serverSocket = new DatagramSocket(port);
        } catch(IOException e) {
            Log.emerg("Could not create ServerSocket in GELFServer::create(): " + e.toString());
            return false;
        }

        return true;
    }

    /**
     * Listens on the formerly created (create()) socket and returns
     * unzipped (GZIP) raw message that can be parsed to a GELFMessage.
     *
     * @return Received message
     * @throws SocketException
     * @throws IOException
     */
    public DatagramPacket listen() throws SocketException, IOException {

        // Create buffer.
        byte[] receiveData = new byte[MAX_PACKET_SIZE];
        DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);

        // Reveive and fill buffer.
        serverSocket.receive(receivedPacket);

        return receivedPacket;
    }

}
