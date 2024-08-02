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

import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;

import java.util.Map;
import java.util.Set;

public interface ImmutableMessage extends Indexable, Acknowledgeable {

    static ImmutableMessage wrap(Message message) {
        return new SerializationMemoizingMessage(message);
    }

    // Overriding Indexable#getId, because there it's deprecated, but in the context of a Message, it still makes sense
    @Override
    String getId();

    Set<IndexSet> getIndexSets();

    Map<String, Object> getFields();

    String getMessage();

    Object getField(String key);

    String getSource();

    Set<String> getStreamIds();
}
