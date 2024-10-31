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
package org.graylog2.indexer.messages;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Wraps a {@link Message} by making it immutable and caching the result of {@link #serialize(SerializationContext)}
 * calls.
 * <p>
 * For more details about the caching behavior, see {@link #serialize(SerializationContext)}
 */
public class SerializationMemoizingMessage implements ImmutableMessage {
    private static final Logger LOG = LoggerFactory.getLogger(SerializationMemoizingMessage.class);
    private static final Logger RATE_LIMITED_LOG = RateLimitedLogFactory.createRateLimitedLog(
            LOG, 1, Duration.ofMinutes(1));

    private final Message delegate;

    private volatile SoftReference<CacheEntry> lastSerializationResult;

    private static class CacheEntry {
        private final byte[] serializedBytes;
        private final ObjectMapper objectMapper;
        private final Meter invalidTimeStampMeter;

        CacheEntry(byte[] serializedBytes, ObjectMapper objectMapper, Meter invalidTimeStampMeter) {
            this.serializedBytes = serializedBytes;
            this.objectMapper = objectMapper;
            this.invalidTimeStampMeter = invalidTimeStampMeter;
        }

        public byte[] serializedBytes() {
            return serializedBytes;
        }

        public ObjectMapper objectMapper() {
            return objectMapper;
        }

        public Meter invalidTimeStampMeter() {
            return invalidTimeStampMeter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final CacheEntry that = (CacheEntry) o;
            return Objects.equals(objectMapper, that.objectMapper)
                    && Objects.equals(invalidTimeStampMeter, that.invalidTimeStampMeter)
                    && Objects.deepEquals(serializedBytes, that.serializedBytes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(objectMapper, invalidTimeStampMeter, Arrays.hashCode(serializedBytes));
        }
    }

    public SerializationMemoizingMessage(Message delegate) {
        this.delegate = delegate;
    }

    /**
     * Serializes a message to JSON and memoizes the result as a {@link SoftReference}.
     * <p>
     * As long as the same {@link SerializationContext#objectMapper()} is used in consecutive calls, the result will
     * be memoized. If a context with a different object mapper is used, the memoized serialized byte array will be
     * replaced with a newly serialized value.
     * <p>
     * The serialization result will be maintained as a {@link SoftReference}. If the JVM experiences memory pressure,
     * the memoized value might be cleared, and consecutive calls will cause the serialization operation to execute
     * again. In that case, a warning will be logged.
     *
     * @param context Context required to perform the serialization
     * @return The serialized value. May return a memoized value according to the rules described above.
     * @throws IOException If serialization goes wrong.
     */
    @Override
    public synchronized byte[] serialize(SerializationContext context)
            throws IOException {

        final var objectMapper = context.objectMapper();
        final var invalidTimestampMeter = context.invalidTimestampMeter();

        CacheEntry cachedEntry = null;
        if (lastSerializationResult != null) {
            cachedEntry = lastSerializationResult.get();
            if (cachedEntry == null) {
                RATE_LIMITED_LOG.warn("The JVM cleared a cached serialized message because of memory pressure. " +
                        "This has a performance impact. Please adjust the memory configuration to assign more " +
                        "heap memory to the JVM.");
            }
        }

        if (cachedEntry == null || !cachedEntry.objectMapper().equals(objectMapper)) {
            final var tsMeter = new Meter();
            final var serializedBytes = delegate.serialize(SerializationContext.of(objectMapper, tsMeter));
            cachedEntry = new CacheEntry(serializedBytes, objectMapper, tsMeter);
            this.lastSerializationResult = new SoftReference<>(cachedEntry);
        }

        invalidTimestampMeter.mark(cachedEntry.invalidTimeStampMeter().getCount());
        return cachedEntry.serializedBytes();
    }

    // only straight-forward delegations below this line

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SerializationMemoizingMessage serializationMemoizingMessage)) {
            return false;
        }
        return delegate.equals(serializationMemoizingMessage.delegate);
    }

    @Override
    @Deprecated
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getMessageId() {
        return delegate.getMessageId();
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public DateTime getReceiveTime() {
        return delegate.getReceiveTime();
    }

    @Override
    public Map<String, Object> toElasticSearchObject(ObjectMapper objectMapper, @Nonnull Meter invalidTimestampMeter) {
        return delegate.toElasticSearchObject(objectMapper, invalidTimestampMeter);
    }

    @Override
    public DateTime getTimestamp() {
        return delegate.getTimestamp();
    }

    @Override
    public boolean supportsFailureHandling() {
        return delegate.supportsFailureHandling();
    }

    @Override
    public ImmutableSet<IndexSet> getIndexSets() {
        return ImmutableSet.copyOf(delegate.getIndexSets());
    }

    @Override
    public ImmutableMap<String, Object> getFields() {
        return ImmutableMap.copyOf(delegate.getFields());
    }

    @Override
    public String getMessage() {
        return delegate.getMessage();
    }

    @Override
    public Object getField(String key) {
        return delegate.getField(key);
    }

    @Override
    public String getSource() {
        return delegate.getSource();
    }

    @Override
    public ImmutableSet<String> getStreamIds() {
        return ImmutableSet.copyOf(delegate.getStreamIds());
    }

    @Override
    public Object getMessageQueueId() {
        return delegate.getMessageQueueId();
    }
}
