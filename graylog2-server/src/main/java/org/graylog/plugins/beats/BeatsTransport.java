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
package org.graylog.plugins.beats;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.assistedinject.Assisted;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.security.encryption.EncryptedValueService;

import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

import static org.graylog2.shared.utilities.StringUtils.f;

public class BeatsTransport extends AbstractTcpTransport {
    static final String CK_MAX_EVENT_SIZE = "max_event_size";
    private static final int USE_CLUSTER_DEFAULT = 0;

    private final ClusterConfigService clusterConfigService;

    @Inject
    public BeatsTransport(@Assisted Configuration configuration,
                          EventLoopGroup eventLoopGroup,
                          EventLoopGroupFactory eventLoopGroupFactory,
                          NettyTransportConfiguration nettyTransportConfiguration,
                          ThroughputCounter throughputCounter,
                          LocalMetricRegistry localRegistry,
                          TLSProtocolsConfiguration tlsConfiguration,
                          EncryptedValueService encryptedValueService,
                          ClusterConfigService clusterConfigService) {
        super(configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration, encryptedValueService);
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>(super.getCustomChildChannelHandlers(input));
        final int maxEventSize = maxEventSize();
        handlers.put("beats", () -> new BeatsFrameDecoder(maxEventSize));

        return handlers;
    }

    /**
     * The input's own limit if set to a positive value, otherwise the cluster-wide default. Resolved when the input
     * starts.
     */
    @VisibleForTesting
    int maxEventSize() {
        final int configured;
        try {
            configured = configuredMaxEventSize(configuration);
        } catch (ConfigurationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return configured > 0 ? configured : defaults(clusterConfigService).maxEventSize();
    }

    /**
     * Validates the input's max. event size. Used by the Beats inputs to reject invalid values when they are saved.
     */
    static void checkConfiguration(Configuration configuration) throws ConfigurationException {
        configuredMaxEventSize(configuration);
    }

    /**
     * Reads the raw configured value, because {@link Configuration#getInt} silently truncates values outside the
     * int range, e.g. turning 4294968320 into 1024.
     */
    private static int configuredMaxEventSize(Configuration configuration) throws ConfigurationException {
        final Object value = configuration.getSource().get(CK_MAX_EVENT_SIZE);
        if (value == null) {
            return USE_CLUSTER_DEFAULT;
        }
        if (!(value instanceof Number number)) {
            throw new ConfigurationException(f("Max. event size must be a number, but is \"%s\".", value));
        }
        final long size = number.longValue();
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new ConfigurationException(f("Max. event size must be between 0 and %d bytes, but is %d.",
                    Integer.MAX_VALUE, size));
        }
        return (int) size;
    }

    private static BeatsInputDefaultsConfig defaults(ClusterConfigService clusterConfigService) {
        return clusterConfigService.getOrDefault(BeatsInputDefaultsConfig.class, BeatsInputDefaultsConfig.DEFAULT_VALUES);
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<BeatsTransport> {
        @Override
        BeatsTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractTcpTransport.Config {
        private final ClusterConfigService clusterConfigService;

        @Inject
        public Config(ClusterConfigService clusterConfigService) {
            this.clusterConfigService = clusterConfigService;
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = super.getRequestedConfiguration();
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(5044);
            }
            // Defaults to 0 instead of the current cluster-wide value, so that inputs follow later changes of the
            // cluster-wide default unless they explicitly override it.
            cr.addField(new NumberField(
                    CK_MAX_EVENT_SIZE,
                    "Max. event size",
                    USE_CLUSTER_DEFAULT,
                    f("Maximum size in bytes of a single event. Larger events are rejected and the connection is closed. " +
                                    "Set to 0 to use the Beats inputs default from System > Configurations " +
                                    "(currently %d bytes).",
                            defaults(clusterConfigService).maxEventSize()),
                    ConfigurationField.Optional.OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE));
            return cr;
        }
    }
}
