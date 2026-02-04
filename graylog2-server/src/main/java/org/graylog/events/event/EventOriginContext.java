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
    private static final String MONGODB_AGGREGATION = String.join(":", URN, "aggregation:mongodb");

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

    public static String mongodbAggregation(String database, String collection, String fromTime, String toTime) {
        checkArgument("database", database);
        checkArgument("collection", collection);
        checkArgument("fromTime", fromTime);
        checkArgument("toTime", toTime);

        return String.join(":", MONGODB_AGGREGATION, database, collection, fromTime, toTime);
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

    public static Optional<MongoDBAggregatOriginContext> parseMongoDBContext(String url) {
        if (url.startsWith(MONGODB_AGGREGATION)) {
            final String[] tokens = url.split(":", -1);  // Use -1 to preserve empty strings
            if (tokens.length < 7) {
                return Optional.empty();
            }
            // Format: urn:graylog:aggregation:mongodb:database:collection:fromTime:toTime
            // Tokens: [0]=urn, [1]=graylog, [2]=aggregation, [3]=mongodb, [4]=database, [5]=collection, [6+]=timestamps
            String database = tokens[4];
            String collection = tokens[5];

            // Reconstruct timestamps which may contain colons (ISO-8601)
            // Everything from token 6 to the last one forms the timestamps
            // Split them by finding the last occurrence that makes sense
            StringBuilder fromTimeBuilder = new StringBuilder();
            StringBuilder toTimeBuilder = new StringBuilder();

            // Simple approach: assume fromTime and toTime are separated at the middle
            // Better approach: ISO-8601 typically has fixed format, reconstruct properly
            if (tokens.length == 7) {
                // Simple case: no colons in timestamps
                return Optional.of(MongoDBAggregatOriginContext.create(database, collection, tokens[6], ""));
            } else if (tokens.length >= 8) {
                // Reconstruct: assume last token is part of toTime, rest belong to fromTime or toTime
                // ISO-8601: 2024-01-15T10:30:00.000Z has 3 colons
                // We have tokens[6...n], need to split into two timestamps
                // Heuristic: each ISO timestamp has ~3 colons, so split accordingly
                int midpoint = 6 + (tokens.length - 6) / 2;
                fromTimeBuilder.append(tokens[6]);
                for (int i = 7; i < midpoint; i++) {
                    fromTimeBuilder.append(":").append(tokens[i]);
                }
                toTimeBuilder.append(tokens[midpoint]);
                for (int i = midpoint + 1; i < tokens.length; i++) {
                    toTimeBuilder.append(":").append(tokens[i]);
                }
                return Optional.of(MongoDBAggregatOriginContext.create(
                        database, collection, fromTimeBuilder.toString(), toTimeBuilder.toString()));
            }
        }
        return Optional.empty();
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

    @AutoValue
    public static abstract class MongoDBAggregatOriginContext {
        public abstract String database();

        public abstract String collection();

        public abstract String fromTime();

        public abstract String toTime();

        public static MongoDBAggregatOriginContext create(String database, String collection,
                                                         String fromTime, String toTime) {
            return new AutoValue_EventOriginContext_MongoDBAggregatOriginContext(
                    database, collection, fromTime, toTime);
        }
    }
}
