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



import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elasticsearch.common.netty.channel.ChannelException;
import org.graylog2.Core;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFUDPInput implements MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(GELFUDPInput.class);

    private static final String NAME = "GELF UDP";
    
    private Core graylogServer;
    private InetSocketAddress socketAddress;
    
    @Override
    public void initialize(Map<String, String> configuration, GraylogServer graylogServer) {
        this.graylogServer = (Core) graylogServer;
        this.socketAddress = new InetSocketAddress(
                    configuration.get("listen_address"),
                    Integer.parseInt(configuration.get("listen_port"))
        );

        spinUp();
    }
    
    private void spinUp() {
        
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                .setNameFormat("input-gelfudp-worker-%d")
                .build());
        
        final ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(workerThreadPool));

        bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(
                graylogServer.getConfiguration().getUdpRecvBufferSizes())
        );
        bootstrap.setPipelineFactory(new GELFUDPPipelineFactory(graylogServer));

        try {
            bootstrap.bind(socketAddress);
            LOG.info("Started UDP GELF server on {}", socketAddress);
        } catch (ChannelException e) {
            LOG.error("Could not bind UDP GELF server to address " + socketAddress, e);
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