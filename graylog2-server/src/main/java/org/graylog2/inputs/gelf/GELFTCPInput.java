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

import org.graylog2.plugin.inputs.*;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.graylog2.plugin.GraylogServer;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFTCPInput extends MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(GELFTCPInput.class);

    public static final String NAME = "GELF TCP";


    @Override
    public void configure(Configuration config, GraylogServer graylogServer) throws ConfigurationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void launch() throws MisfireException {
        /*final ExecutorService bossThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-gelftcp-boss-%d")
                        .build());

        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-gelftcp-worker-%d")
                        .build());

        ServerBootstrap tcpBootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(bossThreadPool, workerThreadPool)
        );

        tcpBootstrap.shutdown();

        tcpBootstrap.setPipelineFactory(new GELFTCPPipelineFactory((Core) server));

        SocketAddress socketAddress = config.get("listen_address").asSocketAddress();

        try {
            tcpBootstrap.bind(socketAddress);
            LOG.info("Started TCP GELF server on {}", socketAddress);
        } catch (ChannelException e) {
            LOG.error("Could not bind TCP GELF server to address " + socketAddress, e);
        }*/
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

}
