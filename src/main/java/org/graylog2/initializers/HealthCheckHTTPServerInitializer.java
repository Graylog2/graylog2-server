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

package org.graylog2.initializers;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.graylog2.healthchecks.HealthCheckServerPipelineFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * HealthCheckHTTPServerInitializer.java: 19.06.2012 16:43:44
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class HealthCheckHTTPServerInitializer implements Initializer {

    private int port;

    public HealthCheckHTTPServerInitializer(int port) {
        this.port = port;
    }

    @Override
    public void initialize() {
        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()
             )
        );

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new HealthCheckServerPipelineFactory());

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(this.port));

    }

}
