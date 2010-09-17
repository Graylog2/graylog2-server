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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import org.graylog2.Log;
import org.graylog2.database.MongoBridge;
import org.graylog2.messagehandlers.common.MessageCounterHook;
import org.graylog2.messagehandlers.common.ReceiveHookManager;

import org.json.simple.*;

/**
 * GELFClient.java: Jun 23, 2010 7:15:12 PM
 *
 * Handling a GELF client message consisting of only one UDP message.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class SimpleGELFClientHandler extends GELFClientHandlerBase implements GELFClientHandlerIF {

    /**
     * Representing a GELF client consisting of only one UDP message.
     * 
     * @param clientMessage The raw data the GELF client sent. (JSON string)
     * @param threadName The name of the GELFClientHandlerThread that called this.
     * @throws DataFormatException
     * @throws UnsupportedEncodingException
     * @throws InvalidGELFCompressionMethodException
     * @throws IOException
     */
    public SimpleGELFClientHandler(DatagramPacket clientMessage, String threadName) throws DataFormatException, UnsupportedEncodingException, InvalidGELFCompressionMethodException, IOException {

        // Determine compression type.
        int type = GELF.getGELFType(clientMessage);

        // Decompress.
        switch (type) {
            // Decompress ZLIB
            case GELF.TYPE_ZLIB:
                Inflater decompresser = new Inflater();
                decompresser.setInput(clientMessage.getData(), 0, clientMessage.getLength());
                int finalLength = decompresser.inflate(clientMessage.getData());
                this.clientMessage = new String(clientMessage.getData(), 0, finalLength, "UTF-8");
                break;

            // Decompress GZIP
            case GELF.TYPE_GZIP:
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[clientMessage.getLength()]; // CHECK THIS STUFF HERE
                GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(clientMessage.getData()));
                for (int bytesRead; (bytesRead = in.read(buffer)) != -1;) {
                    out.write(buffer, 0, bytesRead);
                }
                this.clientMessage = new String(out.toByteArray(), "UTF-8");

                System.out.println("MAGIC NUMBER: " + GZIPInputStream.GZIP_MAGIC); //////////// REMOVE ME
                break;
            default:
                throw new UnsupportedEncodingException();
        }
        
    }
    
    /**
     * Handles the client: Decodes JSON, Stores in MongoDB, ReceiveHooks
     * 
     * @return boolean
     */
    public boolean handle() {
        try {
            JSONObject json = this.getJSON(this.clientMessage.toString());
            if (json == null) {
                Log.warn("JSON is null/could not be parsed (invalid JSON) - clientMessage was: " + this.clientMessage);
                return false;
            }

            // Fills properties with values from JSON.
            try { this.parse(json); } catch(Exception e) {
                Log.warn("Could not parse GELF JSON: " + e.toString() + " - clientMessage was: " + this.clientMessage);
                return false;
            }

            // Store in MongoDB.
            // Connect to database.
            MongoBridge m = new MongoBridge();


            // Log if we are in debug mode.
            Log.info("Got GELF message: " + message.toString());

            // Insert message into MongoDB.
            m.insertGelfMessage(message);

            // This is doing the upcounting for RRD.
            ReceiveHookManager.postProcess(new MessageCounterHook());
        } catch(Exception e) {
            Log.warn("Could not handle GELF client: " + e.toString());
            return false;
        }

        return true;
    }

}
