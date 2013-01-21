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

package org.graylog2.inputs.syslog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.graylog2.Core;
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
 * Syslog UDP input implemented using boring old blocking IO techology. Its no sexy - it just has 4x throughput, comparing to netty 3.5 udp
 * 
 * @author Oleg Anastasyev<oa@odnoklassniki.ru>
 */
public class SyslogUDPBlockingInput implements MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogUDPBlockingInput.class);

    private static final String NAME = "Syslog UDP";

    private Core graylogServer;
    private InetSocketAddress socketAddress;

    private SyslogProcessor processor;
    private final Meter receivedMessages = Metrics.newMeter(SyslogUDPBlockingInput.class, "ReceivedMessages", "messages", TimeUnit.SECONDS);

    @Override
    public void initialize(Map<String, String> configuration, GraylogServer graylogServer) {
        this.graylogServer = (Core) graylogServer;
        this.socketAddress = new InetSocketAddress(
                configuration.get("listen_address"),
                Integer.parseInt(configuration.get("listen_port"))
        );

        this.processor = new SyslogProcessor((Core) graylogServer);
        spinUp();
    }

    private void spinUp() {       
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                .setNameFormat("input-syslogudp-worker-%d")
                .setDaemon(true)
                .build());
        
        try {
            final DatagramSocket dgs = new DatagramSocket(socketAddress);
            int threadCount = Runtime.getRuntime().availableProcessors() * 2;
            
            while (threadCount-- > 0) {
                workerThreadPool.submit(new Runnable()
                {
                    private DatagramPacket packet = new DatagramPacket(new byte[graylogServer.getConfiguration().getUdpRecvBufferSizes()], graylogServer.getConfiguration().getUdpRecvBufferSizes());
                    
                    @Override
                    public void run()
                    {
                        while (true) {
                            try {
                                dgs.receive(packet);
                                receivedMessages.mark();

                                InetSocketAddress remoteAddress = (InetSocketAddress) packet.getSocketAddress();

                                processor.messageReceived(new String(packet.getData(), packet.getOffset(), packet.getLength()), remoteAddress.getAddress());
                                
                            } catch (IOException e) {
                                LOG.error("Could not recv Syslog UDP data to address " + socketAddress, e);
                            } catch (BufferOutOfCapacityException e) {
                                LOG.error("Process Buffer is out of capacity Syslog UDP data to address " + socketAddress, e);
                            }
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
