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

    /**
     * Returns the size of this indexable in bytes as used for <em>output</em> traffic accounting; it is
     * summed into the cluster output-traffic counter and persisted on the indexed document as
     * {@link org.graylog2.plugin.Message#FIELD_GL2_ACCOUNTED_MESSAGE_SIZE}, the figure used for
     * output-based license accounting.
     * <p>
     * Whether the value is counted at all is governed separately by {@link #isAccounted()}.
     */
    long getSize();


    /**
     * Returns the size of this indexable in bytes as used for <em>input</em> traffic accounting — the
     * size of the data as originally received at the input, before processing.
     * <p>
     * The value is summed into the cluster input-indexed-traffic counter and is the figure used for
     * input-based license accounting. The default of {@code 0} reflects that a generic indexable has no
     * associated input message (for example, internally generated indexables);
     * {@link org.graylog2.plugin.Message} overrides this to report the size recorded at decode time.
     * <p>
     * Whether the value is counted at all is governed separately by {@link #isAccounted()}.
     */
    default long getInputMessageSize() {
        return 0L;
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

    /**
     * Whether this indexable is counted in license/traffic accounting.
     * <p>
     * When {@code false}, the indexable is excluded from <em>both</em> the output- and input-traffic
     * counters, so it counts against neither output-based nor input-based license traffic. Defaults to
     * {@code true}.
     */
    default boolean isAccounted() {
        return true;
    }
}
