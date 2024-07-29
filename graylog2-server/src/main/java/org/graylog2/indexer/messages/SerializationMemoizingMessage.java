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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import jakarta.annotation.Nonnull;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class SerializationMemoizingMessage implements ImmutableMessage {
    private static final Logger LOG = LoggerFactory.getLogger(SerializationMemoizingMessage.class);
    private static final Logger RATE_LIMITED_LOG = RateLimitedLogFactory.createRateLimitedLog(
            LOG, 1, Duration.ofSeconds(20));

    private final Message delegate;

    private final Cache<ObjectMapper, CacheEntry> serializedBytesCache;

    private record CacheEntry(byte[] serializedBytes, Meter invalidTimeStampMeter) {}

    public SerializationMemoizingMessage(Message delegate) {
        this(delegate, LOG.isDebugEnabled());
    }

    public SerializationMemoizingMessage(Message delegate, boolean enableCacheStats) {
        this.delegate = delegate;

        final var cacheBuilder = CacheBuilder.newBuilder().weakKeys().softValues();
        if (enableCacheStats) {
            this.serializedBytesCache = cacheBuilder.recordStats().build();
        } else {
            this.serializedBytesCache = cacheBuilder.build();
        }
    }

    @Override
    public synchronized byte[] serialize(ObjectMapper objectMapper, @Nonnull Meter invalidTimestampMeter)
            throws JsonProcessingException {

        try {
            final var cacheEntry = serializedBytesCache.get(objectMapper, () -> {
                        final var tsMeter = new Meter();
                        return new CacheEntry(delegate.serialize(objectMapper, tsMeter), tsMeter);
                    }
            );
            invalidTimestampMeter.mark(cacheEntry.invalidTimeStampMeter().getCount());
            if (RATE_LIMITED_LOG.isDebugEnabled()) {
                if (cacheStats().evictionCount() > 0) {
                    RATE_LIMITED_LOG.debug("The JVM cleared a cached serialized message because of memory pressure.");
                }
            }
            return cacheEntry.serializedBytes();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof JsonProcessingException jsonProcessingException) {
                throw jsonProcessingException;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * See {@link Cache#stats()}
     */
    CacheStats cacheStats() {
        return serializedBytesCache.stats();
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
