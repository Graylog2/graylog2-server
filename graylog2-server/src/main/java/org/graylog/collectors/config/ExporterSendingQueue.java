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
package org.graylog.collectors.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector/blob/main/exporter/exporterhelper/README.md">exporterhelper</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class ExporterSendingQueue {
    public enum Sizer {
        @JsonProperty("requests")
        REQUESTS,
        @JsonProperty("items")
        ITEMS,
        @JsonProperty("bytes")
        BYTES
    }

    @JsonProperty("enabled")
    public abstract boolean enabled();

    @JsonProperty("num_consumers")
    public abstract int numConsumers();

    @JsonProperty("wait_for_result")
    public abstract boolean waitForResult();

    @JsonProperty("block_on_overflow")
    public abstract boolean blockOnOverflow();

    @JsonProperty("sizer")
    public abstract Sizer sizer();

    @JsonProperty("queue_size")
    public abstract int queueSize();

    @JsonProperty("batch")
    public abstract Optional<Batch> batch();

    @JsonProperty("storage")
    public abstract Optional<String> storage();

    public static Builder builder() {
        return new AutoValue_ExporterSendingQueue.Builder()
                .enabled(true)
                .numConsumers(10)
                .waitForResult(false)
                .blockOnOverflow(false)
                .sizer(Sizer.REQUESTS)
                .queueSize(1000)
                .storage(null)
                .batch(null);
    }

    public abstract Builder toBuilder();

    public static ExporterSendingQueue createDefault() {
        return builder().build();
    }

    public static ExporterSendingQueue createDefaultWithStorage(String storage) {
        return builder().storage(storage).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder enabled(boolean enabled);

        public abstract Builder numConsumers(int numConsumers);

        public abstract Builder waitForResult(boolean waitForResult);

        public abstract Builder blockOnOverflow(boolean blockOnOverflow);

        public abstract Builder sizer(Sizer sizer);

        public abstract Builder queueSize(int queueSize);

        public abstract Builder storage(@Nullable String storage);

        public abstract Builder batch(@Nullable Batch batch);

        public abstract ExporterSendingQueue build();
    }

    @AutoValue
    public abstract static class Batch {
        @JsonProperty("flush_timeout")
        public abstract Duration flushTimeout();

        // Should be less than or equal to the sending_queue.queue_size if sending_queue.batch.sizer matches sending_queue.sizer.
        @JsonProperty("min_size")
        public abstract int minSize();

        @JsonProperty("max_size")
        public abstract int maxSize();

        @JsonProperty("sizer")
        public abstract Optional<Sizer> sizer();

        public static Builder builder() {
            return new AutoValue_ExporterSendingQueue_Batch.Builder()
                    .flushTimeout(Duration.ofMillis(200))
                    .minSize(8192)
                    .maxSize(0)
                    .sizer(null);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder flushTimeout(Duration flushTimeout);

            public abstract Builder minSize(int minSize);

            public abstract Builder maxSize(int maxSize);

            public abstract Builder sizer(@Nullable Sizer sizer);

            public abstract Batch build();
        }
    }
}
