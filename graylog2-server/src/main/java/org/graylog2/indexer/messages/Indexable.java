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
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

public interface Indexable {
    /**
     * Returns the id to address the document in Elasticsearch.
     * Depending on the implementation this might return a {@link com.eaio.uuid.UUID} or {@link de.huxhorn.sulky.ulid.ULID}
     * This method should only be used where backwards compatibility is needed.
     * Newer code should use {@link Indexable#getMessageId()} instead.
     */
    @Deprecated
    String getId();

    /**
     * Returns the id to address the document in Elasticsearch.
     * The message id is represented as a {@link de.huxhorn.sulky.ulid.ULID}
     */
    String getMessageId();

    long getSize();

    /**
     * Returns the input message size in bytes. This is the raw payload size at the transport
     * layer, as recorded in {@link org.graylog2.plugin.Message#FIELD_GL2_INPUT_MESSAGE_SIZE}.
     * Falls back to {@link #getSize()} if no input message size was recorded.
     */
    default long getInputMessageSize() {
        return getSize();
    }

    DateTime getReceiveTime();

    Map<String, Object> toElasticSearchObject(ObjectMapper objectMapper, @Nonnull final Meter invalidTimestampMeter);

    DateTime getTimestamp();

    /**
     * Serializes the object to a form that can be sent to the indexer, e.g. JSON.
     * <p>
     * The default implementation will just call {@link #toElasticSearchObject(ObjectMapper, Meter)}
     *
     * @param context Context required to perform the serialization
     * @return an array of bytes that can be sent to the indexer
     * @throws IOException if serializing the object fails
     */
    default byte[] serialize(SerializationContext context) throws IOException {
        return context.objectMapper().writeValueAsBytes(
                toElasticSearchObject(context.objectMapper(), context.invalidTimestampMeter())
        );
    }

    /**
     * Guides the failure handling framework when deciding whether this particular
     * message should be accepted for the further failure processing. By default
     * disabled for all messages.
     */
    default boolean supportsFailureHandling() {
        return false;
    }
}
