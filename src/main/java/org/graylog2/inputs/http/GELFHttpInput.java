/**
 * Copyright 2012 Kay Roepke <kroepke@googlemail.com>
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
package org.graylog2.inputs.http;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.log4j.Logger;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.inputs.MessageInput;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GELFHttpInput implements MessageInput {

    private static final Logger LOG = Logger.getLogger(GELFHttpInput.class);

    @Override
    public void initialize(final Configuration configuration, final Core graylogServer) {
        final InetSocketAddress socketAddress = new InetSocketAddress(configuration.getHttpListenAddress(), configuration.getHttpListenPort());

        final ExecutorService bossExecutor = Executors.newCachedThreadPool(
            new BasicThreadFactory.Builder()
                .namingPattern("input-gelfhttp-boss-%d")
                .build());

        final ExecutorService workerExecutor = Executors.newCachedThreadPool(
            new BasicThreadFactory.Builder()
                .namingPattern("input-gelfhttp-worker-%d")
                .build());

        final ServerBootstrap httpBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(bossExecutor, workerExecutor)
        );
        httpBootstrap.setPipelineFactory(new GELFHttpPipelineFactory(graylogServer));

        try {
            httpBootstrap.bind(socketAddress);
            LOG.info("Started HTTP GELF server on " + socketAddress);
        } catch (final ChannelException e) {
            LOG.fatal("Could not bind HTTP GELF server to address " + socketAddress, e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                httpBootstrap.releaseExternalResources();
            }
        });
    }

    @Override
    public String getName() {
        return "HTTP GELF";
    }
}
