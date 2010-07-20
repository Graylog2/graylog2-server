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

/**
 * GELFThread.java: Lennart Koopmann <lennart@scopeport.org> | Jun 23, 2010 6:58:07 PM
 */

package org.graylog2.messagehandlers.gelf;

import org.graylog2.Log;

import java.net.*;
import java.io.*;

public class GELFServer {
    private static final int MAX_PACKET_SIZE = 4096;

    private DatagramSocket serverSocket = null;

    public GELFServer() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                Log.info("Closing server socket.");
                tearDown();
            }
        }));
    }

    public boolean create(int port) {
        try {
            this.serverSocket = new DatagramSocket(port);
        } catch(IOException e) {
            Log.emerg("Could not create ServerSocket in GELFServer::create(): " + e.toString());
            return false;
        }

        return true;
    }

    public String listen() {
        try {
            byte[] receiveData = new byte[MAX_PACKET_SIZE];
            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivedPacket);
            return new String(receivedPacket.getData(), 0, receivedPacket.getLength());
        } catch(IOException e) {
            Log.emerg("Could not receive on ServerSocket in GELFServer::listen(): " + e.toString());
        }

        return null;
    }

    public void tearDown() {
        this.serverSocket.close();
    }
}
