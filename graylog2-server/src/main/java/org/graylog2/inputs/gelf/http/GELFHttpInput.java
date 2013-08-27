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
package org.graylog2.inputs.gelf.http;

import org.graylog2.plugin.inputs.*;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.graylog2.plugin.GraylogServer;

import java.util.Map;

public class GELFHttpInput extends MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(GELFHttpInput.class);

    public static final String NAME = "GELF HTTP";

    @Override
    public void configure(Configuration config, GraylogServer graylogServer) throws ConfigurationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void launch() throws MisfireException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stop() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isExclusive() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /*@Override
    public void initialize(final Map<String, String> configuration, GraylogServer graylogServer) {
        final InetSocketAddress socketAddress = new InetSocketAddress(
                configuration.get("listen_address"),
                Integer.parseInt(configuration.get("listen_port"))
        );

        final ExecutorService bossExecutor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("input-gelfhttp-boss-%d")
                .build());

        final ExecutorService workerExecutor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("input-gelfhttp-worker-%d")
                .build());

        final ServerBootstrap httpBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(bossExecutor, workerExecutor)
        );
        httpBootstrap.setPipelineFactory(new GELFHttpPipelineFactory((Core) graylogServer));

        try {
            httpBootstrap.bind(socketAddress);
            LOG.debug("Started HTTP GELF server on {}", socketAddress);
        } catch (final ChannelException e) {
            LOG.error("Could not bind HTTP GELF server to address " + socketAddress, e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                httpBootstrap.releaseExternalResources();
            }
        });
    }*/

}
