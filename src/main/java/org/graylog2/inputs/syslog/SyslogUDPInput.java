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

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.graylog2.Configuration;
import org.graylog2.GraylogServer;
import org.graylog2.inputs.MessageInput;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

/**
 * SyslogUDPInput.java: 30.04.2012 00:02:48
 *
 * Describe me.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogUDPInput implements MessageInput {

    private static final Logger LOG = Logger.getLogger(SyslogUDPInput.class);

    private static final String NAME = "Syslog UDP";

    private GraylogServer graylogServer;
    private InetSocketAddress socketAddress;

    @Override
    public void initialize(Configuration configuration, GraylogServer graylogServer) {
        this.graylogServer = graylogServer;
        this.socketAddress = new InetSocketAddress(
                configuration.getSyslogListenAddress(),
                configuration.getSyslogListenPort()
        );

        spinUp();
    }

    private void spinUp() {
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool();
        final ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(workerThreadPool));

        bootstrap.setPipelineFactory(new SyslogPipelineFactory(graylogServer));

        try {
            bootstrap.bind(socketAddress);
            LOG.info("Started UDP Syslog server on " + socketAddress);
        } catch (ChannelException e) {
            LOG.fatal("Could not bind Syslog UDP server to address " + socketAddress, e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

}
