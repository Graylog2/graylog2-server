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
package org.graylog2.inputs.gelf.http;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.inputs.gelf.GELFInputBase;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.inputs.MisfireException;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GELFHttpInput extends GELFInputBase {

    private static final Logger LOG = LoggerFactory.getLogger(GELFHttpInput.class);

    public static final String NAME = "GELF HTTP";
    private final MetricRegistry metricRegistry;

    @Inject
    public GELFHttpInput(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void initialize(Configuration configuration) {
        super.initialize(configuration);

        // Register throughput counter gauges.
        for(Map.Entry<String,Gauge<Long>> gauge : throughputCounter.gauges().entrySet()) {
            metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), gauge.getKey()), gauge.getValue());
        }

        // Register connection counter gauges.
        metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), "open_connections"), connectionCounter.gaugeCurrent());
        metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), "total_connections"), connectionCounter.gaugeTotal());
    }

    @Override
    public void launch(Buffer processBuffer) throws MisfireException {
        final ExecutorService bossExecutor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-" + getId() + "-gelfhttp-boss-%d")
                        .build());

        final ExecutorService workerExecutor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("input-" + getId() + "-gelfhttp-worker-%d")
                        .build());

        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(bossExecutor, workerExecutor)
        );
        bootstrap.setPipelineFactory(new GELFHttpPipelineFactory(metricRegistry, processBuffer, this, throughputCounter, connectionCounter));

        try {
            channel = ((ServerBootstrap) bootstrap).bind(socketAddress);
            LOG.debug("Started GELF HTTP input on {}", socketAddress);
        } catch (Exception e) {
            String msg = "Could not bind GELF HTTP input to address " + socketAddress;
            LOG.error(msg, e);
            throw new MisfireException(msg);
        }
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "http://graylog2.org/resources/documentation/sending/gelfhttp";
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest r = super.getRequestedConfiguration();
        r.addField(new BooleanField("enable_cors", "Enable CORS", true, "Input sends CORS headers to satisfy browser security policies"));

        return r;
    }
}
