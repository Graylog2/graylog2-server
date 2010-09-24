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

import java.net.DatagramPacket;
import org.graylog2.Log;


/**
 * GELFClientHandlerThread.java: Jun 23, 2010 7:09:40 PM
 *
 * Thread that handles a GELF client.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFClientHandlerThread extends Thread {

    private DatagramPacket receivedGelfSentence;

    /**
     * Thread that handles a GELF client.
     *
     * @param receivedGelfSentence Raw GELF message
     */
    public GELFClientHandlerThread(DatagramPacket receivedGelfSentence) {
        this.receivedGelfSentence = receivedGelfSentence;
    }

    /**
     * Start the thread. Exits when the client has been completely handled.
     */
    @Override public void run() {
        try {
            GELFClientHandlerIF client = null;
            if (GELF.isChunkedMessage(this.receivedGelfSentence)) {
                Log.info("Received message is chunked. Handling now.");
                client = new ChunkedGELFClientHandler(this.receivedGelfSentence);
            } else {
                Log.info("Received message is not chunked. Handling now.");
                client = new SimpleGELFClientHandler(this.receivedGelfSentence);
            }
            client.handle();
        } catch (InvalidGELFTypeException e) {
            Log.crit("Invalid GELF type in message: " + e.toString());
        } catch (InvalidGELFHeaderException e) {
            Log.crit("Invalid GELF header in message: " + e.toString());
        } catch (InvalidGELFCompressionMethodException e) {
            Log.crit("Invalid compression method of GELF message: " + e.toString());
        } catch (java.util.zip.DataFormatException e) {
            Log.crit("Invalid compression data format in GELF message: " + e.toString());
        } catch (java.io.UnsupportedEncodingException e) {
            Log.crit("Invalid enconding of GELF message: " + e.toString());
        } catch (java.io.IOException e) {
            Log.crit("IO Error while handling GELF message: " + e.toString());
        }
    }

}
