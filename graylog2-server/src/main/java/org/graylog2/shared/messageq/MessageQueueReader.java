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
import com.google.common.util.concurrent.Service;

import javax.inject.Inject;

import static com.codahale.metrics.MetricRegistry.name;

public interface MessageQueueReader extends Service {

    @AutoValue
    abstract class Metrics {
        public static class Provider implements javax.inject.Provider<Metrics> {
            private final MetricRegistry metricRegistry;

            @Inject
            public Provider(MetricRegistry metricRegistry) {
                this.metricRegistry = metricRegistry;
            }

            @Override
            public Metrics get() {
                return Metrics.builder()
                        .readMessages(metricRegistry.meter(name(MessageQueueReader.class, "read-messages")))
                        .readBytes(metricRegistry.meter(name(MessageQueueReader.class, "read-bytes")))
                        .build();
            }
        }

        public abstract Meter readMessages();

        public abstract Meter readBytes();

        public static Builder builder() {
            return new AutoValue_MessageQueueReader_Metrics.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder readMessages(Meter readMessages);

            public abstract Builder readBytes(Meter readBytes);

            public abstract Metrics build();
        }
    }
}
