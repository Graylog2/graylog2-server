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
package org.graylog2.inputs.transports;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.GeneratorTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.stream.Collectors;


public class InternalMetricsTransport extends GeneratorTransport {

    public static final String CK_SLEEP = "sleep";

    private final MetricRegistry metricRegistry;
    private final ObjectMapper objectMapper;
    private final int sleepMs;

    @AssistedInject
    public InternalMetricsTransport(@Assisted Configuration configuration, EventBus eventBus, ObjectMapper objectMapper, MetricRegistry metricRegistry) {
        super(eventBus, configuration);
        this.metricRegistry = metricRegistry;
        this.objectMapper = objectMapper;

        sleepMs = configuration.intIsSet(CK_SLEEP) ? configuration.getInt(CK_SLEEP) : 0;

    }

    @Override
    protected RawMessage produceRawMessage(MessageInput input) {
        final byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(captureInternalMetrics());
            final RawMessage raw = new RawMessage(payload);
            Thread.sleep(sleepMs);
            return raw;
        } catch (JsonProcessingException e) {
            //log.error("Unable to serialize generator state", e);
        } catch (InterruptedException ignored) {
        }
        return null;
    }

    private InternalMetrics captureInternalMetrics() {
        Map<String, Object> gauges = metricRegistry.getGauges()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue()));

        return InternalMetrics.builder()
                .timestamp(new DateTime(DateTimeZone.UTC))
                .gauges(gauges)
                .build();
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<InternalMetricsTransport> {
        @Override
        InternalMetricsTransport create(Configuration configuration);

        @Override
        InternalMetricsTransport.Config getConfig();
    }

    @ConfigClass
    public static class Config extends GeneratorTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest c = super.getRequestedConfiguration();
            c.addField(new NumberField(
                    CK_SLEEP,
                    "Sleep time",
                    25,
                    "How many milliseconds to sleep between generating messages.",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE
            ));
            return c;
        }
    }
}
