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
import com.cronutils.utils.VisibleForTesting;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

public class SerializationMemoizingMessage implements ImmutableMessage {
    private final Message delegate;

    private volatile WeakReference<byte[]> memoizedBytes = new WeakReference<>(null);

    @VisibleForTesting
    volatile int serializationCounter = 0;

    public SerializationMemoizingMessage(Message delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized byte[] serialize(ObjectMapper objectMapper, @Nonnull Meter invalidTimestampMeter)
            throws JsonProcessingException {
        final var bytes = memoizedBytes.get();
        if (bytes != null) {
            return bytes;
        }

        final Meter tsMeter;
        if (serializationCounter++ == 0) {
            tsMeter = invalidTimestampMeter;
        } else {
            // dummy meter we because we don't want to count invalid timestamps multiple times
            tsMeter = new Meter();
        }
        final var serializedBytes = delegate.serialize(objectMapper, tsMeter);
        memoizedBytes = new WeakReference<>(serializedBytes);
        return serializedBytes;
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
