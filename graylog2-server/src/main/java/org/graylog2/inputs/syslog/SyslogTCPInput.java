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

import org.graylog2.plugin.inputs.*;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.graylog2.plugin.GraylogServer;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogTCPInput implements MessageInput {
    
    private static final Logger LOG = LoggerFactory.getLogger(SyslogTCPInput.class);

    public static final String NAME = "Syslog TCP";

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
    public void setId(String id) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getId() {
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

    /*private Core graylogServer;
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
        final ExecutorService bossThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                .setNameFormat("input-syslogtcp-boss-%d")
                .build());
        
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                .setNameFormat("input-syslogtcp-worker-%d")
                .build());

        ServerBootstrap tcpBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(bossThreadPool, workerThreadPool)
        );

        tcpBootstrap.setPipelineFactory(new SyslogTCPPipelineFactory(this.graylogServer));

        try {
            tcpBootstrap.bind(socketAddress);
            LOG.info("Started TCP syslog server on {}", socketAddress);
        } catch (ChannelException e) {
            LOG.error("Could not bind TCP syslog server to address " + socketAddress, e);
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
    }*/

}
