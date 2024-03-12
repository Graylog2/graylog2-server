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

import jakarta.inject.Singleton;
import org.joda.time.DateTime;

import java.util.Map;

// Intentionally package-private to enforce usage of injected MessageFactory.
@Singleton
class DefaultMessageFactory implements MessageFactory {
    @Override
    public Message createMessage(final String message, final String source, final DateTime timestamp) {
        return new Message(message, source, timestamp);
    }

    @Override
    public Message createMessage(final Map<String, Object> fields) {
        return new Message(fields);
    }

    @Override
    public Message createMessage(final String id, Map<String, Object> newFields) {
        return new Message(id, newFields);
    }
}
