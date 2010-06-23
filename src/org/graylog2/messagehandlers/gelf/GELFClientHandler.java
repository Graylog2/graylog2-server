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
 * GELFClientHandler.java: Lennart Koopmann <lennart@scopeport.org> | Jun 23, 2010 7:13:03 PM
 */

package org.graylog2.messagehandlers.gelf;

import org.graylog2.Log;

import java.net.Socket;
import java.io.*;

public class GELFClientHandler {
    private Socket socket = null;
    private String threadName = null;

    private String clientMessage = null;

    GELFClientHandler(Socket clientSocket, String threadName) {
        this.socket = clientSocket;
        this.threadName = threadName;
    }

    public void handle(){
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.clientMessage = in.readLine();
            GELFClient client = new GELFClient(clientMessage, threadName);
            client.handle();
        } catch(InterruptedIOException e) {
            Log.warn("Client " + socket.getRemoteSocketAddress() + " timeout after " + GELF.CLIENT_TIMEOUT + "ms");
        } catch(IOException e) {
            Log.emerg("Could not handle client " + socket.getRemoteSocketAddress() + " in GELFClientHandler::handle(): " + e.toString());
        } finally {
            // Make sure that the socket is closed.
            try { socket.close(); } catch(IOException e) {}
        }

    }

}
