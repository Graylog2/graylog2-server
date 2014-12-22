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
package org.graylog2.inputs.transports;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.syslog.tcp.SyslogTCPFramingRouterHandler;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.channel.ChannelHandler;

import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;

public class SyslogTcpTransport extends TcpTransport {
    @AssistedInject
    public SyslogTcpTransport(@Assisted Configuration configuration,
                              @Named("bossPool") Executor bossPool,
                              ThroughputCounter throughputCounter,
                              ConnectionCounter connectionCounter,
                              LocalMetricRegistry localRegistry) {
        super(configuration,
                bossPool,
                executorService("worker", "syslog-tcp-transport-worker-%d", localRegistry),
                throughputCounter,
                connectionCounter,
                localRegistry);
    }

    private static Executor executorService(final String executorName, final String threadNameFormat, final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNameFormat).build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                metricRegistry,
                name(HttpTransport.class, executorName, "executor-service"));
    }


    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getFinalChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> finalChannelHandlers = Maps.newLinkedHashMap();

        finalChannelHandlers.putAll(super.getFinalChannelHandlers(input));

        // Replace the "framer" channel handler inserted by the parent.
        finalChannelHandlers.put("framer", new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                return new SyslogTCPFramingRouterHandler(maxFrameLength, delimiter);
            }
        });

        return finalChannelHandlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<SyslogTcpTransport> {
        SyslogTcpTransport create(Configuration configuration);
    }

    @ConfigClass
    public static class Config extends TcpTransport.Config {

    }
}
