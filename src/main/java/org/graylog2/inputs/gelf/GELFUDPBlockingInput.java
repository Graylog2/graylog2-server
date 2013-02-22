/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.inputs.gelf;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.graylog2.Core;
import org.graylog2.gelf.GELFChunkManager;
import org.graylog2.gelf.GELFMessage;
import org.graylog2.gelf.GELFProcessor;
import org.graylog2.gelf.MessageParseException;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;

/**
 * GELF UDP input implemented boring old blocking IO techology. Its no sexy - it just has 4x throughput, comparing to netty udp
 * 
 * @author Oleg Anastasyev<oa@odnoklassniki.ru>
 */
public class GELFUDPBlockingInput implements MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(GELFUDPBlockingInput.class);

    private static final String NAME = "GELF UDP";

    private Core server;
    private InetSocketAddress socketAddress;

    private GELFProcessor processor;
    private GELFChunkManager chunkManager;
    private final Meter receivedMessages = Metrics.newMeter(GELFUDPBlockingInput.class, "ReceivedMessages", "messages", TimeUnit.SECONDS);
    private final Meter dispatchedMessageChunk = Metrics.newMeter(GELFUDPBlockingInput.class, "DispatchedMessagesChunks", "messages", TimeUnit.SECONDS);
    private final Meter dispatchedUnchunkedMessage = Metrics.newMeter(GELFUDPBlockingInput.class, "DispatchedNonChunkedMessages", "messages", TimeUnit.SECONDS);

    @Override
    public void initialize(Map<String, String> configuration, GraylogServer graylogServer) {
        this.server = (Core) graylogServer;
        this.socketAddress = new InetSocketAddress(
                configuration.get("listen_address"),
                Integer.parseInt(configuration.get("listen_port"))
        );

        this.processor = new GELFProcessor((Core) graylogServer);
        this.chunkManager = server.getGELFChunkManager();
        spinUp();
    }

    private void spinUp() {       
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                .setNameFormat("input-gelfudp-worker-%d")
                .setDaemon(true)
                .build());
        
        try {
            final DatagramSocket dgs = new DatagramSocket(socketAddress);
            int threadCount = Runtime.getRuntime().availableProcessors() * 4;
            
            while (threadCount-- > 0) {
                workerThreadPool.submit(new Runnable()
                {
                    private DatagramPacket packet = new DatagramPacket(new byte[server.getConfiguration().getUdpRecvBufferSizes()], server.getConfiguration().getUdpRecvBufferSizes());
                    
                    @Override
                    public void run()
                    {
                        try {
                            while (true) {
                                try {
                                    dgs.receive(packet);
                                    receivedMessages.mark();

                                    InetSocketAddress remoteAddress = (InetSocketAddress) packet.getSocketAddress();

                                    try {
                                        GELFMessage msg = new GELFMessage(packet.getData(), packet.getOffset(), packet.getLength());

                                        switch(msg.getGELFType()) {
                                        case CHUNKED:
                                            dispatchedMessageChunk.mark();
                                            chunkManager.insert(msg);
                                            break;
                                        case ZLIB:
                                        case GZIP:
                                        case UNCOMPRESSED:
                                        case UNSUPPORTED:
                                            dispatchedUnchunkedMessage.mark();
                                            processor.messageReceived(msg);
                                            break;
                                        }
                                    } catch (MessageParseException e) {
                                        LOG.error("Cannot parse packet received from "+packet.getAddress()+", starting from "+Arrays.toString(Arrays.copyOfRange(packet.getData(), packet.getOffset(),packet.getOffset()+11)),e);
                                    }
                                } catch (IOException e) {
                                    LOG.error("Could not recv GELF UDP data to address " + socketAddress, e);
                                } catch (BufferOutOfCapacityException e) {
                                    LOG.error("Process Buffer is out of capacity Syslog UDP data to address " + socketAddress, e);
                                }
                            }
                        } catch (Throwable e) {
                            LOG.error("GELF UDP is dying because of unexpected exception",e);
                        }
                    }
                });
            }
            LOG.info("Started UDP Syslog server on {}", socketAddress);
        } catch (SocketException e) {
            LOG.error("Could not bind Syslog UDP server to address " + socketAddress, e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        // Built in input. This is just for plugin compat. No special configuration required.
        return Maps.newHashMap();
    }
    
}
