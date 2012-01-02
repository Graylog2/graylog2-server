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

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

/**
 * GELFThread.java: Jun 23, 2010 6:58:07 PM
 *
 * Server that can listen for GELF messages.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFServer {

    private static final Logger LOG = Logger.getLogger(GELFServer.class);

    /**
     * The maximum packet size. (8192 is reasonable UDP limit)
     */
    public static final int MAX_PACKET_SIZE = 8192;

    /**
     * Create the configured servers to accept GELF messages on: UDP and optionally nul-byte delimited TCP, and HTTP.
     *
     * @param socketAddress The {@link InetSocketAddress} to bind to
     * @param useTcp true if the TCP server should be started as well
     * @param httpListenPort if non-zero the port on which to listen to HTTP requests
     * @return boolean true if all requested servers could be bound, false otherwise
     */
    public static boolean create(InetSocketAddress socketAddress, boolean useTcp, int httpListenPort) {
        final ExecutorService bossThreadPool = Executors.newCachedThreadPool();
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool();
        
        // UDP GELF server
        
        final ConnectionlessBootstrap udpBootstrap = new ConnectionlessBootstrap(
                new NioDatagramChannelFactory(workerThreadPool)
                );
        udpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new UdpGELFHandler());
            }
        });
        
        // TCP GELF server
        ServerBootstrap tcpBootstrap = null;
        if (useTcp) {
            tcpBootstrap = new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                            bossThreadPool, workerThreadPool)
                    );
            tcpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    ChannelPipeline p = Channels.pipeline();
                    p.addLast("framer", new DelimiterBasedFrameDecoder(2 * 1024 * 1024,
                            Delimiters.nulDelimiter()));
                    p.addLast("handler", new TcpGELFHandler());
                    return p;
                }
            });
        }
        
        // HTTP GELF server
        ServerBootstrap httpBootstrap  = null;
        if (httpListenPort != 0) {
            httpBootstrap = new ServerBootstrap(
                    new NioServerSocketChannelFactory(bossThreadPool, workerThreadPool)
                    );
            httpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    ChannelPipeline pipeline = Channels.pipeline();
                    pipeline.addLast("decoder", new HttpRequestDecoder());
                    pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
                    pipeline.addLast("encoder", new HttpResponseEncoder());
                    pipeline.addLast("deflater", new HttpContentCompressor());
                    pipeline.addLast("handler", new HttpGELFHandler());

                    return pipeline;
                }
            });
        }
        try {
            udpBootstrap.bind(socketAddress);
            LOG.info("Started UDP GELF server on " + socketAddress);
            if (useTcp) {
                tcpBootstrap.bind(socketAddress);
                LOG.info("Started TCP GELF server on " + socketAddress);
            }
            if (httpListenPort != 0) {
                httpBootstrap.bind(new InetSocketAddress(socketAddress.getAddress(), httpListenPort));
                LOG.info("Started HTTP GELF server on " + socketAddress.getAddress() + ":" + httpListenPort);
            }
        } catch (ChannelException e) {
            LOG.fatal("Could not bind GELF server to address " + socketAddress, e);
            return false;
        }

        return true;
    }

}
