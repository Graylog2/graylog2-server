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
import org.graylog2.forwarders.Forwarder;
import org.graylog2.messagehandlers.common.HostUpsertHook;
import org.graylog2.messagehandlers.common.MessageCountUpdateHook;
import org.graylog2.messagehandlers.common.MessageParserHook;
import org.graylog2.messagehandlers.common.ReceiveHookManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.zip.DataFormatException;
import org.graylog2.messagehandlers.common.RealtimeCollectionUpdateHook;
import org.graylog2.messagequeue.MessageQueue;

/**
 * GELFClient.java: Jun 23, 2010 7:15:12 PM
 *
 * Handling a GELF client message consisting of only one UDP message.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SimpleGELFClientHandler extends GELFClientHandlerBase implements GELFClientHandlerIF {

    private static final Logger LOG = Logger.getLogger(SimpleGELFClientHandler.class);

    private String amqpReceiverQueue = null;
    private boolean preParsed = false;

    /**
     * Representing a GELF client consisting of only one UDP message.
     * 
     * @param clientMessage The raw data the GELF client sent. (JSON string)
     * @throws DataFormatException
     * @throws UnsupportedEncodingException
     * @throws InvalidGELFCompressionMethodException
     * @throws IOException
     */
    public SimpleGELFClientHandler(Object clientMessage) throws DataFormatException, InvalidGELFCompressionMethodException, IOException {

        if (clientMessage instanceof DatagramPacket) {
            DatagramPacket msg = (DatagramPacket) clientMessage;
            // Determine compression type.
            int type = GELF.getGELFType(msg.getData());

            this.message.setRaw(msg.getData());

            // Decompress.
            switch (type) {
                // Decompress ZLIB
                case GELF.TYPE_ZLIB:
                    LOG.debug("Handling ZLIB compressed SimpleGELFClient");
                    this.clientMessage = Tools.decompressZlib(msg.getData());
                    break;

                // Decompress GZIP
                case GELF.TYPE_GZIP:
                    LOG.debug("Handling GZIP compressed SimpleGELFClient");
                    this.clientMessage = Tools.decompressGzip(msg.getData());
                    break;

                // Unsupported encoding if not handled by prior cases.
                default:
                    throw new UnsupportedEncodingException();
            }
        } else if(clientMessage instanceof String) {
            this.clientMessage = (String) clientMessage;
        } else if(clientMessage instanceof GELFMessage) {
            this.message = (GELFMessage) clientMessage;
            this.preParsed = true;
        }
        
    }
    
    /**
     * Handles the client: Decodes JSON, Stores in Indexer, ReceiveHooks
     * 
     * @return boolean
     */
    public boolean handle() {
        try {
            // Parse JSON to GELFMessage if necessary.
            if (!this.preParsed) {
                try {
                    this.parse();
                } catch (Exception e) {
                    LOG.warn("Could not parse GELF JSON: " + e.getMessage() + " - clientMessage was: " + this.clientMessage, e);
                    return false;
                }
            }
            
            if (!this.message.allRequiredFieldsSet()) {
                LOG.info("GELF message is not complete. Version, host and short_message must be set.");
                return false;
            }
        	
            // Add AMQP receiver queue as additional field if set.
            if (this.getAmqpReceiverQueue() != null) {
                this.message.addAdditionalData("_amqp_queue", this.getAmqpReceiverQueue());
            }

            if (!this.message.convertedFromSyslog()) {
                LOG.debug("Got GELF message: " + this.message.toString());
            }

            // Blacklisted?
            if (this.message.blacklisted(Blacklist.fetchAll())) {
                return true;
            }

            // PreProcess message based on filters. Insert message into indexer.
            ReceiveHookManager.preProcess(new MessageParserHook(), message);
            if(!message.getFilterOut()) {
                // Add message to queue and post-process if it was successful.
                if (MessageQueue.getInstance().add(message)) {
                    // Update periodic counts collection.
                    ReceiveHookManager.postProcess(new MessageCountUpdateHook(), message);

                    // Counts up host in hosts collection.
                    ReceiveHookManager.postProcess(new HostUpsertHook(), message);

                    // Update realtime collection-
                    ReceiveHookManager.postProcess(new RealtimeCollectionUpdateHook(), message);
                }
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

    /**
     * @return the amqpReceiverQueue
     */
    public String getAmqpReceiverQueue() {
        return this.amqpReceiverQueue;
    }

    /**
     * @param amqpReceiverQueue the amqpReceiverQueue to set
     */
    public void setAmqpReceiverQueue(String amqpReceiverQueue) {
        this.amqpReceiverQueue = amqpReceiverQueue;
    }

}
