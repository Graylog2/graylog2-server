/**
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
 */
package org.graylog2.inputs.gelf.udp;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.inputs.gelf.GELFInputBase;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MisfireException;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFUDPInput extends GELFInputBase {

    private static final Logger LOG = LoggerFactory.getLogger(GELFUDPInput.class);

    public static final String NAME = "GELF UDP";
    private final MetricRegistry metricRegistry;
    private final GELFChunkManager gelfChunkManager;

    @Inject
    public GELFUDPInput(MetricRegistry metricRegistry,
                        GELFChunkManager gelfChunkManager) {
        this.metricRegistry = metricRegistry;
        this.gelfChunkManager = gelfChunkManager;
    }

    @Override
    public void initialize(Configuration configuration) {
        super.initialize(configuration);

        // Register throughput counter gauges.
        for(Map.Entry<String,Gauge<Long>> gauge : throughputCounter.gauges().entrySet()) {
            metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), gauge.getKey()), gauge.getValue());
        }
    }

    @Override
    public void launch(Buffer processBuffer) throws MisfireException {
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-" + getId() + "-gelfudp-worker-%d")
                        .build());

        bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(workerThreadPool));
        bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(8192));
        bootstrap.setPipelineFactory(new GELFUDPPipelineFactory(metricRegistry, gelfChunkManager, processBuffer, this, throughputCounter));
        bootstrap.setOption("receiveBufferSize", getRecvBufferSize());

        try {
            channel = ((ConnectionlessBootstrap) bootstrap).bind(socketAddress);
            LOG.info("Started GELF UDP input on {}", socketAddress);
        } catch (Exception e) {
            String msg = "Could not bind UDP GELF input to address " + socketAddress;
            LOG.error(msg, e);
            throw new MisfireException(msg);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

}