/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.inputs.raw.tcp;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.inputs.raw.RawInputBase;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.inputs.raw.udp.RawUDPPipelineFactory;
import org.graylog2.inputs.syslog.tcp.SyslogTCPPipelineFactory;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.inputs.MisfireException;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RawTCPInput extends RawInputBase {

    private static final Logger LOG = LoggerFactory.getLogger(RawUDPInput.class);

    public static final String NAME = "Raw/Plaintext TCP";

    public static final String CK_USE_NULL_DELIMITER = "use_null_delimiter";

    @Override
    public void launch() throws MisfireException {
        // Register throughput counter gauges.
        for(Map.Entry<String,Gauge<Long>> gauge : throughputCounter.gauges().entrySet()) {
            core.metrics().register(MetricRegistry.name(getUniqueReadableId(), gauge.getKey()), gauge.getValue());
        }

        // Register connection counter gauges.
        core.metrics().register(MetricRegistry.name(getUniqueReadableId(), "open_connections"), connectionCounter.gaugeCurrent());
        core.metrics().register(MetricRegistry.name(getUniqueReadableId(), "total_connections"), connectionCounter.gaugeTotal());

        final ExecutorService bossThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-" + inputId + "-rawtcp-boss-%d")
                        .build());

        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-" + inputId + "-rawtcp-worker-%d")
                        .build());

        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(bossThreadPool, workerThreadPool)
        );

        bootstrap.setPipelineFactory(new RawTCPPipelineFactory(core, config, this, throughputCounter, connectionCounter));

        try {
            channel = ((ServerBootstrap) bootstrap).bind(socketAddress);
            LOG.info("Started raw TCP input on {}", socketAddress);
        } catch (ChannelException e) {
            String msg = "Could not bind raw TCP input to address " + socketAddress;
            LOG.error(msg, e);
            throw new MisfireException(msg, e);
        }
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest x = super.getRequestedConfiguration();

        x.addField(
                new BooleanField(
                        CK_USE_NULL_DELIMITER,
                        "Null frame delimiter?",
                        false,
                        "Use null byte as frame delimiter? Default is newline."
                )
        );

        return x;
    }


    @Override
    public String getName() {
        return NAME;
    }

}
