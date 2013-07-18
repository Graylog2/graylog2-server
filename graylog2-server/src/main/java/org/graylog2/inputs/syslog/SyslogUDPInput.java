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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.Core;
import org.graylog2.plugin.inputs.*;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.graylog2.plugin.GraylogServer;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogUDPInput extends SyslogInputBase implements MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogUDPInput.class);

    public static final String NAME = "Syslog UDP";

    private Core core;
    private String inputId;
    private Configuration config;
    private InetSocketAddress socketAddress;
    private ConnectionlessBootstrap bootstrap;

    @Override
    public void configure(Configuration config, GraylogServer graylogServer) throws ConfigurationException {
        this.core = (Core) graylogServer;
        this.config = config;

        if (!checkConfig(config)) {
            throw new ConfigurationException();
        }

        this.socketAddress = new InetSocketAddress(
                config.getString(CK_BIND_ADDRESS),
                (int) config.getInt(CK_PORT)
        );
    }

    @Override
    public void launch() throws MisfireException {
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-" + inputId + "-syslogudp-worker-%d")
                        .build());

        bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(workerThreadPool));

        bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(
                core.getConfiguration().getUdpRecvBufferSizes())
        );
        bootstrap.setPipelineFactory(new SyslogPipelineFactory(core, config));

        try {
            bootstrap.bind(socketAddress);
            LOG.info("Started UDP Syslog server on {}", socketAddress);
        } catch (ChannelException e) {
            LOG.error("Could not bind Syslog UDP server to address " + socketAddress, e);
        }
    }

    @Override
    public void stop() {
        if (bootstrap != null) {
            bootstrap.shutdown();
        }
    }

    @Override
    public void setId(String id) {
        this.inputId = id;
    }

    @Override
    public String getId() {
        return inputId;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

}
