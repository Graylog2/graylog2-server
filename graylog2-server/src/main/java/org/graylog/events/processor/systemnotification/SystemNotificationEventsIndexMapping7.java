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
package org.graylog.events.processor.systemnotification;

import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.EventsIndexMapping7;

public class SystemNotificationEventsIndexMapping7 extends EventsIndexMapping7 {
    @Override
    protected ImmutableMap<String, Object> fieldProperties() {
        return map()
                .put("id", map()
                        .put("type", "keyword")
                        .build())
                .put("event_definition_type", map()
                        .put("type", "keyword")
                        .build())
                .put("event_definition_id", map()
                        .put("type", "keyword")
                        .build())
                .put("timestamp", map()
                        .put("type", "date")
                        // Use the same format we use for the "message" mapping to make sure we
                        // can use the search.
                        .put("format", dateFormat())
                        .build())
                .put("message", map()
                        .put("type", "text")
                        .put("analyzer", "standard")
                        .put("norms", false)
                        .put("fields", map()
                                .put("keyword", map()
                                        .put("type", "keyword")
                                        .build())
                                .build())
                        .build())
                .put("key", map()
                        .put("type", "keyword")
                        .build())
                .put("key_tuple", map()
                        .put("type", "keyword")
                        .build())
                .put("priority", map()
                        .put("type", "long")
                        .build())
                .put("alert", map()
                        .put("type", "boolean")
                        .build())
                .put("fields", map()
                        .put("type", "object")
                        .put("dynamic", true)
                        .build())
                .put("group_by_fields", map()
                        .put("type", "object")
                        .put("dynamic", true)
                        .build())
                /* TODO: Enable the typed fields once we decided if that's the way to go
                .put("fields_typed", map()
                        .put("type", "object")
                        .put("properties", map()
                                .put("long", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .put("double", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .put("boolean", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .put("ip", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .build())
                        .build())
                 */
                .build();
    }
}
