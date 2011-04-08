/**
 * Copyright 2010, 2011 Lennart Koopmann <lennart@socketfeed.com>
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
import org.graylog2.Tools;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.database.MongoBridge;
import org.graylog2.forwarders.Forwarder;
import org.graylog2.messagehandlers.common.HostUpsertHook;
import org.graylog2.messagehandlers.common.MessageCounterHook;
import org.graylog2.messagehandlers.common.MessageParserHook;
import org.graylog2.messagehandlers.common.ReceiveHookManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.zip.DataFormatException;

/**
 * ChunkedGELFClient.java: Sep 14, 2010 6:38:38 PM
 *
 * Handling a GELF client message consisting on more than one UDP message.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ChunkedGELFClientHandler extends GELFClientHandlerBase implements GELFClientHandlerIF {

    private static final Logger LOG = Logger.getLogger(ChunkedGELFClientHandler.class);


    /**
     * Representing a GELF client based on more than one UDP message.
     *
     * @param clientMessage The raw data the GELF client sent. (JSON string)
     * @throws UnsupportedEncodingException
     * @throws InvalidGELFHeaderException
     * @throws IOException 
     * @throws DataFormatException
     * @throws InvalidGELFCompressionMethodException
     */
    public ChunkedGELFClientHandler(DatagramPacket clientMessage) throws UnsupportedEncodingException, InvalidGELFHeaderException, IOException, DataFormatException, InvalidGELFCompressionMethodException {
        GELFHeader header = GELF.extractGELFHeader(clientMessage);

        GELFClientChunk chunk = new GELFClientChunk();
        chunk.setRaw(clientMessage.getData(), clientMessage.getLength());
        chunk.setHash(header.getHash());
        chunk.setSequenceCount(header.getSequenceCount());
        chunk.setSequenceNumber(header.getSequenceNumber());
        chunk.setData(GELF.extractData(clientMessage));
        chunk.setArrival((int) (System.currentTimeMillis()/1000));

        // Insert the chunk.
        ChunkedGELFMessage possiblyCompleteMessage = null;
        try {
            possiblyCompleteMessage = ChunkedGELFClientManager.getInstance().insertChunk(chunk);
        } catch (InvalidGELFChunkException e) {
            throw new InvalidGELFHeaderException(e.toString());
        } catch (ForeignGELFChunkException e) {
            throw new InvalidGELFHeaderException(e.toString());
        }

        LOG.info("Got GELF message chunk: " + chunk.toString());

        // Catch the full message data if all chunks are complete.
        if (possiblyCompleteMessage != null) {
            ChunkedGELFMessage completeMessage = possiblyCompleteMessage;

            byte[] data = null;
            String hash = null;
            try {
                hash = completeMessage.getHash();
                LOG.info("Chunked GELF message <" + hash + "> complete. Handling now.");
                data = completeMessage.getData();
            } catch (IncompleteGELFMessageException e) {
                LOG.warn("Tried to fetch information from incomplete chunked GELF message", e);
                return;
            }

            try {
                // Decompress and store in this.clientMessage
                decompress(data, hash);

                // Store message chunks for easy later forwarding.
                this.message.storeMessageChunks(completeMessage.getChunkMap());
                this.message.setIsChunked(true);
            } catch(IOException e) {
                LOG.warn("Error while trying to decompress complete message: " + e.toString());
            } finally {
                // Remove message from chunk manager as it is being handled next.
                ChunkedGELFClientManager.getInstance().dropMessage(hash);
            }
        }
    }

    private void decompress(byte[] data, String hash) throws InvalidGELFCompressionMethodException, IOException {
        // Determine compression type.
        int type = GELF.getGELFType(data);
        // Decompress.
        switch (type) {
            // Decompress ZLIB
            case GELF.TYPE_ZLIB:
                LOG.info("Chunked GELF message <" + hash + "> is ZLIB compressed.");
                this.clientMessage = Tools.decompressZlib(data);
                break;
            case GELF.TYPE_GZIP:
                LOG.info("Chunked GELF message <" + hash + "> is GZIP compressed.");
                this.clientMessage = Tools.decompressGzip(data);
                break;
            default:
                throw new InvalidGELFCompressionMethodException("Unknown compression type.");
        }
    }

    /**
     * Handles the client: Decodes JSON, Stores in MongoDB, ReceiveHooks
     *
     * @return boolean
     */
    public boolean handle() {
        // Don't handle if message is incomplete.
        if (this.clientMessage == null) {
            return true;
        }

        try {
             // Fills properties with values from JSON.
            try { this.parse(); } catch(Exception e) {
                LOG.warn("Could not parse GELF JSON: " + e.getMessage() + " - clientMessage was: " + this.clientMessage, e);
                return false;
            }

            // Store in MongoDB.
            // Connect to database.
            MongoBridge m = new MongoBridge();

            if (!this.message.convertedFromSyslog()) {
                LOG.info("Got GELF message: " + this.message.toString());
            }

            // Blacklisted?
            if (this.message.blacklisted(Blacklist.fetchAll())) {
                return true;
            }

            // PreProcess message based on filters. Insert message into MongoDB.
            ReceiveHookManager.preProcess(new MessageParserHook(), message);
            if(!message.getFilterOut()) {
                m.insertGelfMessage(message);
                // This is doing the upcounting for statistics.
                ReceiveHookManager.postProcess(new MessageCounterHook(), message);

                // Counts up host in hosts collection.
                ReceiveHookManager.postProcess(new HostUpsertHook(), message);
            }

            // Forward.
            int forwardCount = Forwarder.forward(this.message);
            if (forwardCount > 0) {
                LOG.info("Forwarded message to " + forwardCount + " endpoints");
            }
        } catch(Exception e) {
            LOG.warn("Could not handle GELF client: " + e.getMessage(), e);
            return false;
        }

        return true;
    }


}
