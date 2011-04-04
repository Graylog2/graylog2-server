/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.coms>
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

import org.apache.log4j.Logger;

import java.net.DatagramPacket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GELFMainThread.java: Jun 23, 2010 6:43:21 PM
 *
 * Thread responsible for listening for GELF messages.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMainThread extends Thread {

    private static final Logger LOG = Logger.getLogger(GELFMainThread.class);

    private int port = 0;

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Thread responsible for listening for GELF messages.
     *
     * @param port The TCP port to listen on
     */
    public GELFMainThread(int port) {
        this.port = port;
    }

    /**
     * Run the thread. Runs forever!
     */
    @Override public void run() {
        GELFServer server = new GELFServer();
        if (!server.create(this.port)) {
            throw new RuntimeException("Could not start GELF server. Do you have permissions to listen on UDP port " + this.port + "?");
        }

        // Run forever.
        while (true) {
            try {
                // Listen on socket.
                DatagramPacket receivedGelfSentence = server.listen();

                // Skip empty sentences.
                if (receivedGelfSentence.getLength() == 0) {
                    continue;
                }

                // We got a connected client. Start a GELFClientHandlerThread() in our thread pool and wait for next client.
                threadPool.execute(new GELFClientHandlerThread(receivedGelfSentence));
            } catch (Exception e) {
                LOG.error("Skipping GELF client. Error: " + e.getMessage(), e);
            }
        }
    }

}
