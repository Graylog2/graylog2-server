/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin.inputs.transports;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.annotations.VisibleForTesting;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.inputs.transports.netty.ExceptionLoggingChannelHandler;
import org.graylog2.inputs.transports.netty.PromiseFailureHandler;
import org.graylog2.inputs.transports.netty.RawMessageHandler;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.MetricSets;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.util.PacketInformationDumper;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class NettyTransport implements Transport {
    public static final String CK_BIND_ADDRESS = "bind_address";
    public static final String CK_PORT = "port";
    public static final String CK_RECV_BUFFER_SIZE = "recv_buffer_size";
    public static final String CK_NUMBER_WORKER_THREADS = "number_worker_threads";
    private static final int DEFAULT_NUMBER_WORKER_THREADS = Runtime.getRuntime().availableProcessors();

    private static final Logger log = LoggerFactory.getLogger(NettyTransport.class);

    protected final EventLoopGroupFactory eventLoopGroupFactory;
    protected final MetricRegistry localRegistry;

    protected final InetSocketAddress socketAddress;
    protected final ThroughputCounter throughputCounter;
    protected final int workerThreads;
    private final int recvBufferSize;

    @Nullable
    private CodecAggregator aggregator;

    public NettyTransport(Configuration configuration,
                          EventLoopGroupFactory eventLoopGroupFactory,
                          ThroughputCounter throughputCounter,
                          LocalMetricRegistry localRegistry) {
        this.throughputCounter = throughputCounter;

        final String hostname = configuration.getString(CK_BIND_ADDRESS);
        if (hostname != null && configuration.intIsSet(CK_PORT)) {
            this.socketAddress = new InetSocketAddress(hostname, configuration.getInt(CK_PORT));
        } else {
            this.socketAddress = null;
        }
        this.recvBufferSize = configuration.intIsSet(CK_RECV_BUFFER_SIZE)
                ? configuration.getInt(CK_RECV_BUFFER_SIZE)
                : MessageInput.getDefaultRecvBufferSize();

        this.eventLoopGroupFactory = eventLoopGroupFactory;

        this.workerThreads = configuration.getInt(CK_NUMBER_WORKER_THREADS, DEFAULT_NUMBER_WORKER_THREADS);

        this.localRegistry = localRegistry;
        localRegistry.registerAll(MetricSets.of(throughputCounter.gauges()));
    }

    protected ChannelInitializer<? extends Channel> getChannelInitializer(final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlerList) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                final ChannelPipeline p = ch.pipeline();
                Map.Entry<String, Callable<? extends ChannelHandler>> postentry = null;
                for (final Map.Entry<String, Callable<? extends ChannelHandler>> entry : handlerList.entrySet()) {
                    // Handle exceptions at the top of the (bottom-up evaluated) pipeline
                    if (entry.getKey().equals("exception-logger")) {
                        postentry = entry;
                    } else {
                        p.addLast(entry.getKey(), entry.getValue().call());
                    }
                }
                if (postentry != null) {
                    p.addLast(postentry.getKey(), postentry.getValue().call());
                }
            }
        };
    }

    /**
     * Get the local socket address this transport is listening on after being launched.
     *
     * @return the listening address of this transport or {@code null} if the transport hasn't been launched yet.
     */
    @VisibleForTesting
    @Nullable
    public abstract SocketAddress getLocalAddress();

    @Override
    public void setMessageAggregator(@Nullable CodecAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Nullable
    protected CodecAggregator getAggregator() {
        return aggregator;
    }

    /**
     * Subclasses can override this to add additional {@link ChannelHandler channel handlers} to the
     * Netty {@link ChannelPipeline} to support additional features.
     *
     * Some common use cases are to add connection counters or traffic shapers.
     *
     * @param input The {@link MessageInput} for which these channel handlers are being added
     * @return list of initial {@link ChannelHandler channel handlers} to add to the Netty {@link ChannelPipeline channel pipeline}
     */
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getChannelHandlers(final MessageInput input) {
        LinkedHashMap<String, Callable<? extends ChannelHandler>> handlerList = new LinkedHashMap<>();

        handlerList.put("exception-logger", () -> new ExceptionLoggingChannelHandler(input, log));
        handlerList.put("packet-meta-dumper", () -> new PacketInformationDumper(input));
        handlerList.put("output-failure-logger", () -> PromiseFailureHandler.INSTANCE);

        return handlerList;
    }

    /**
     * Subclasses can override this to modify the {@link ChannelHandler channel handlers} at the end of the pipeline for child channels.
     *
     * @param input The {@link MessageInput} for which these child channel handlers are being added
     * @return list of custom {@link ChannelHandler channel handlers} to add to the Netty {@link ChannelPipeline channel pipeline} for child channels
     */
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(final MessageInput input) {
        return new LinkedHashMap<>();
    }

    /**
     * Subclasses can override this to modify the {@link ChannelHandler channel handlers} for child channels.
     *
     * The default handlers in this group are all channel handlers returned by {@link #getCustomChildChannelHandlers(MessageInput)},
     * the optional aggregation handler (e.g. for chunked GELF via UDP) and the {@link RawMessageHandler} (in that order).
     *
     * Usually overriding this method should only be necessary if you have a codec that cannot create a
     * {@link org.graylog2.plugin.journal.RawMessage} for incoming messages at the end of the pipeline.
     *
     * A valid use case would be to insert debug handlers in the middle of the list, though.
     *
     * @param input The {@link MessageInput} for which these child channel handlers are being added
     * @return list of custom {@link ChannelHandler channel handlers} to add to the Netty {@link ChannelPipeline channel pipeline} for child channels
     * @see #getCustomChildChannelHandlers(MessageInput)
     */
    protected abstract LinkedHashMap<String, Callable<? extends ChannelHandler>> getChildChannelHandlers(final MessageInput input);

    protected int getRecvBufferSize() {
        return recvBufferSize;
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    public static class Config implements Transport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = new ConfigurationRequest();

            r.addField(ConfigurationRequest.Templates.bindAddress(CK_BIND_ADDRESS));
            r.addField(ConfigurationRequest.Templates.portNumber(CK_PORT, 5555));
            r.addField(ConfigurationRequest.Templates.recvBufferSize(CK_RECV_BUFFER_SIZE, 1024 * 1024));
            r.addField(new NumberField(
                    CK_NUMBER_WORKER_THREADS,
                    "No. of worker threads",
                    DEFAULT_NUMBER_WORKER_THREADS,
                    "Number of worker threads processing network connections for this input.",
                    // Should be mandatory, but then all existing inputs are failing to start
                    ConfigurationField.Optional.OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE)
            );

            return r;
        }
    }
}
