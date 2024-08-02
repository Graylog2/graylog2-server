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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.shared.messageq.Acknowledgeable;

/**
 * The purpose of this interface is to provide access to certain properties of a {@link Message} while ensuring that
 * it can't be changed anymore. This allows precomputation and re-use of e.g. the serialized JSON of a message (see
 * {@link Indexable#serialize(SerializationContext)} while ensuring that the precomputed value does not become invalid
 * when the message content would be changed.
 * <p>
 * For example, the interface can be used in the output path to compute the space required by a {@link Message} in a
 * batch request to OpenSearch. Batch computation happens early in the process and when the actual index request is to
 * be sent, the precomputed serialized value can be re-used with certainty that the message has not been altered in
 * the meantime.
 */
public interface ImmutableMessage extends Indexable, Acknowledgeable {

    static ImmutableMessage wrap(Message message) {
        return new SerializationMemoizingMessage(message);
    }

    // Overriding Indexable#getId, because there it's deprecated, but in the context of a Message, it still makes sense
    @Override
    String getId();

    ImmutableSet<IndexSet> getIndexSets();

    ImmutableMap<String, Object> getFields();

    String getMessage();

    Object getField(String key);

    String getSource();

    ImmutableSet<String> getStreamIds();
}
