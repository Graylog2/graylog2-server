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



import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.elasticsearch.common.netty.channel.ChannelException;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.inputs.MessageInput;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFUDPInput implements MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(GELFUDPInput.class);

    private static final String NAME = "GELF UDP";
    
    private Core graylogServer;
    private InetSocketAddress socketAddress;
    
    @Override
    public void initialize(Configuration configuration, Core graylogServer) {
        this.graylogServer = graylogServer;
        this.socketAddress = new InetSocketAddress(configuration.getGelfListenAddress(), configuration.getGelfListenPort());

        spinUp();
    }
    
    private void spinUp() {
        
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                .setNameFormat("input-gelfudp-worker-%d")
                .build());
        
        final ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(workerThreadPool));

        bootstrap.setOption("receiveBufferSize", 1048576);
        bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(8192));
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
    
}