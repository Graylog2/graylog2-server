/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports;

import com.codahale.metrics.InstrumentedExecutorService;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;

public class UdpTransport extends NettyTransport {
    private static final Logger LOG = LoggerFactory.getLogger(UdpTransport.class);

    private final Executor workerExecutor;

    @AssistedInject
    public UdpTransport(@Assisted Configuration configuration,
                        ThroughputCounter throughputCounter,
                        LocalMetricRegistry localRegistry) {
        super(configuration, throughputCounter, localRegistry);
        this.workerExecutor = executorService("worker", "udp-transport-worker-%d", localRegistry);
    }

    private static Executor executorService(final String executorName, final String threadNameFormat, final LocalMetricRegistry localRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNameFormat).build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                localRegistry,
                name(UdpTransport.class, executorName, "executor-service"));
    }

    @Override
    public Bootstrap getBootstrap() {
        final ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(workerExecutor));

        final int recvBufferSize = Ints.saturatedCast(getRecvBufferSize());
        LOG.debug("Setting receive buffer size to {} bytes", recvBufferSize);
        bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(recvBufferSize));
        bootstrap.setOption("receiveBufferSize", recvBufferSize);

        return bootstrap;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<UdpTransport> {
        @Override
        UdpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends NettyTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            final int recvBufferSize = Ints.saturatedCast(Size.kilobytes(256L).toBytes());
            r.addField(ConfigurationRequest.Templates.recvBufferSize(CK_RECV_BUFFER_SIZE, recvBufferSize));

            return r;
        }
    }
}
