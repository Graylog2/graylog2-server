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
package org.graylog2.plugin;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.IndexingResultCallback;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * A message factory that we use in tests. Don't use {@link DefaultMessageFactory} in tests because its constructor
 * signature might change in the future.
 */
public class TestMessageFactory implements MessageFactory {
    @Override
    public Message createMessage(String message, String source, DateTime timestamp) {
        return new Message(message, source, timestamp);
    }

    @Override
    public Message createMessage(Map<String, Object> fields) {
        return new Message(fields);
    }

    @Override
    public Message createMessage(String id, Map<String, Object> newFields) {
        return new Message(id, newFields);
    }

    @Override
    public SystemMessage createSystemMessage(IndexSet indexSet, Map<String, Object> fields, @Nullable IndexingResultCallback resultCallback) {
        return new SystemMessage(indexSet, fields, resultCallback);
    }
}
