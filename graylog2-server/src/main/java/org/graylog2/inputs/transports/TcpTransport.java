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
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;

import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jboss.netty.handler.codec.frame.Delimiters.lineDelimiter;
import static org.jboss.netty.handler.codec.frame.Delimiters.nulDelimiter;

public class TcpTransport extends AbstractTcpTransport {
    public static final String CK_USE_NULL_DELIMITER = "use_null_delimiter";
    private static final String CK_MAX_MESSAGE_SIZE = "max_message_size";

    protected final ChannelBuffer[] delimiter;
    protected final int maxFrameLength;

    @AssistedInject
    public TcpTransport(@Assisted Configuration configuration,
                        @Named("bossPool") Executor bossPool,
                        ThroughputCounter throughputCounter,
                        ConnectionCounter connectionCounter,
                        LocalMetricRegistry localRegistry) {
        this(configuration,
                bossPool,
                executorService("worker", "tcp-transport-worker-%d", localRegistry),
                throughputCounter,
                connectionCounter,
                localRegistry);
    }

    protected TcpTransport(final Configuration configuration,
                           final Executor bossPool,
                           final Executor workerPool,
                           final ThroughputCounter throughputCounter,
                           final ConnectionCounter connectionCounter,
                           final LocalMetricRegistry localRegistry) {
        super(configuration, throughputCounter, localRegistry, bossPool, workerPool, connectionCounter);

        final boolean nulDelimiter = configuration.getBoolean(CK_USE_NULL_DELIMITER);
        this.delimiter = nulDelimiter ? nulDelimiter() : lineDelimiter();
        this.maxFrameLength = configuration.getInt(CK_MAX_MESSAGE_SIZE, Config.DEFAULT_MAX_FRAME_LENGTH);
    }

    private static Executor executorService(final String executorName, final String threadNameFormat, final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNameFormat).build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                metricRegistry,
                name(TcpTransport.class, executorName, "executor-service"));
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getFinalChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> finalChannelHandlers = Maps.newLinkedHashMap();

        finalChannelHandlers.put("framer", new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                return new DelimiterBasedFrameDecoder(maxFrameLength, delimiter);
            }
        });
        finalChannelHandlers.putAll(super.getFinalChannelHandlers(input));

        return finalChannelHandlers;
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<TcpTransport> {
        @Override
        TcpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractTcpTransport.Config {
        public static final int DEFAULT_MAX_FRAME_LENGTH = 2 * 1024 * 1024;

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest x = super.getRequestedConfiguration();

            x.addField(
                    new BooleanField(
                            CK_USE_NULL_DELIMITER,
                            "Null frame delimiter?",
                            false,
                            "Use null byte as frame delimiter? Otherwise newline delimiter is used."
                    )
            );
            x.addField(
                    new NumberField(
                            CK_MAX_MESSAGE_SIZE,
                            "Maximum message size",
                            2 * 1024 * 1024,
                            "The maximum length of a message.",
                            ConfigurationField.Optional.OPTIONAL,
                            NumberField.Attribute.ONLY_POSITIVE
                    )
            );

            return x;
        }
    }
}
