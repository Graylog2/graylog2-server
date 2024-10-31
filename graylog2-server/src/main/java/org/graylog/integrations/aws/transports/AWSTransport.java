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
package org.graylog.integrations.aws.transports;

import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.inputs.transports.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.Locale;
import java.util.Map;

public class AWSTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(AWSTransport.class);
    public static final String NAME = "aws-transport";

    private final LocalMetricRegistry localRegistry;
    private final Map<String, Transport.Factory<? extends Transport>> availableTransports;
    private final Configuration configuration;
    private Transport resolvedTransport;

    @Inject
    public AWSTransport(@Assisted final Configuration configuration,
                        EventBus serverEventBus,
                        LocalMetricRegistry localRegistry,
                        Map<String, Transport.Factory<? extends Transport>> availableTransports) {
        super(serverEventBus, configuration);
        this.configuration = configuration;
        this.localRegistry = localRegistry;
        this.availableTransports = availableTransports;
    }

    @Override
    public void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {

        LOG.debug("Start AWS Transport");
        // Load the transport by message type.
        final Transport transport = resolveTransport();
        transport.launch(input, inputFailureRecorder);

        // Keep reference to the transport, so it can be stopped later.
        resolvedTransport = transport;
    }

    @Override
    public void doStop() {

        LOG.debug("Stop AWS Transport");
        if (resolvedTransport == null) {
            LOG.error("A transport was not found with this [{}] instance.",
                    configuration.getString(AWSCodec.CK_AWS_MESSAGE_TYPE));
        }
        resolvedTransport.stop();
    }

    /**
     * Looks up the transport for the {@link AWSMessageType} stored in the input configuration.
     */
    private Transport resolveTransport() throws MisfireException {
        final AWSMessageType awsMessageType = AWSMessageType.valueOf(configuration.getString(AWSCodec.CK_AWS_MESSAGE_TYPE));
        final Transport.Factory<? extends Transport> transportFactory = this.availableTransports.get(awsMessageType.getTransportName());
        if (transportFactory == null) {
            throw new MisfireException(String.format(Locale.ROOT, "A transport with name [%s] could not be found.",
                    awsMessageType.getTransportName()));
        }

        return transportFactory.create(configuration);
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<AWSTransport> {
        @Override
        AWSTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return super.getRequestedConfiguration();
        }
    }
}
