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
package org.graylog.events.event;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EventOriginContext {
    private static final String URN = "urn:graylog";
    private static final String ES_MESSAGE = String.join(":", URN, "message:es");
    private static final String ES_EVENT = String.join(":", URN, "event:es");

    public static String elasticsearchMessage(String indexName, String messageId) {
        checkArgument("indexName", indexName);
        checkArgument("messageId", messageId);

        return String.join(":", ES_MESSAGE, indexName, messageId);
    }

    public static String elasticsearchEvent(String indexName, String eventId) {
        checkArgument("indexName", indexName);
        checkArgument("eventId", eventId);

        return String.join(":", ES_EVENT, indexName, eventId);
    }

    public static Optional<ESEventOriginContext> parseESContext(String url) {
        if (url.startsWith(ES_EVENT) || url.startsWith(ES_MESSAGE)) {
            final String[] tokens = url.split(":");
            if (tokens.length != 6) {
                return Optional.empty();
            }
            return Optional.of(ESEventOriginContext.create(tokens[4], tokens[5]));
        } else {
            return Optional.empty();
        }
    }

    private static void checkArgument(String name, String value) {
        Preconditions.checkArgument(!isNullOrEmpty(value), name + " cannot be null or empty");
    }

    @AutoValue
    public static abstract class ESEventOriginContext {
        public abstract String indexName();

        public abstract String messageId();

        public static ESEventOriginContext create(String indexName, String messageId) {
            return new AutoValue_EventOriginContext_ESEventOriginContext(indexName, messageId);
        }
    }
}
