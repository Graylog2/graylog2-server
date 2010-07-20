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
 * GELFMainThread.java: Lennart Koopmann <lennart@scopeport.org> | Jun 23, 2010 6:43:21 PM
 */

package org.graylog2.messagehandlers.gelf;

import org.graylog2.Log;

public class GELFMainThread extends Thread {

    private int port = 0;

    public GELFMainThread(int port) {
        this.port = port;
    }

    @Override public void run() {
        GELFServer server = new GELFServer();
        if (!server.create(this.port)) {
            System.out.println("Could not start GELF server. Do you have permissions to listen on UDP port " + this.port + "?");
            System.exit(1); // Exit with error.
        }

        // Run forever.
        while (true) {
            try {
                // Listen on socket.
                String receivedGelfSentence = server.listen();
                
                // We got a connected client. Start a GELFClientHandlerThread() and wait for next client.
                GELFClientHandlerThread thread = new GELFClientHandlerThread(receivedGelfSentence);
                thread.start();
            } catch (Exception e) {
                Log.crit("Skipping GELF client. Error: " + e.toString());
                continue;
            }
        }
    }

}
