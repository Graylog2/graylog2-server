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
package org.graylog2.shared.messageq;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.auto.value.AutoValue;

import javax.inject.Inject;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

public interface MessageQueueAcknowledger {

    void acknowledge(Object messageId);

    void acknowledge(List<Object> messageIds);

    @AutoValue
    abstract class Metrics {
        public static class Provider implements javax.inject.Provider<MessageQueueAcknowledger.Metrics> {
            private final MetricRegistry metricRegistry;

            @Inject
            public Provider(MetricRegistry metricRegistry) {
                this.metricRegistry = metricRegistry;
            }

            @Override
            public MessageQueueAcknowledger.Metrics get() {
                return MessageQueueAcknowledger.Metrics.builder()
                        .acknowledgedMessages(
                                metricRegistry.meter(name(MessageQueueAcknowledger.class, "acknowledged-messages")))
                        .build();
            }
        }

        public abstract Meter acknowledgedMessages();

        public static Builder builder() {
            return new AutoValue_MessageQueueAcknowledger_Metrics.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder acknowledgedMessages(Meter acknowledgedMessages);

            public abstract Metrics build();
        }
    }
}
