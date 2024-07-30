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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

public class SerializationMemoizingMessage implements ImmutableMessage {
    private static final Logger LOG = LoggerFactory.getLogger(SerializationMemoizingMessage.class);
    private static final Logger RATE_LIMITED_LOG = RateLimitedLogFactory.createRateLimitedLog(
            LOG, 1, Duration.ofMinutes(1));

    private final Message delegate;

    private volatile SoftReference<CacheEntry> lastSerializationResult;

    private record CacheEntry(byte[] serializedBytes,
                              ObjectMapper objectMapper,
                              Meter invalidTimeStampMeter) {}

    public SerializationMemoizingMessage(Message delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized byte[] serialize(ObjectMapper objectMapper, @Nonnull Meter invalidTimestampMeter)
            throws JsonProcessingException {

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
            final var serializedBytes = delegate.serialize(objectMapper, tsMeter);
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
    public Set<IndexSet> getIndexSets() {
        return delegate.getIndexSets();
    }

    @Override
    public Map<String, Object> getFields() {
        return delegate.getFields();
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
    public Set<String> getStreamIds() {
        return delegate.getStreamIds();
    }

    @Override
    public Object getMessageQueueId() {
        return delegate.getMessageQueueId();
    }
}
