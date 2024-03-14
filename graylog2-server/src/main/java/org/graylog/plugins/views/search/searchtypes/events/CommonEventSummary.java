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
package org.graylog.plugins.views.search.searchtypes.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CommonEventSummary {
    String FIELD_ID = "id";
    String FIELD_STREAMS = "streams";
    String FIELD_EVENT_TIMESTAMP = "timestamp";
    String FIELD_MESSAGE = "message";
    String FIELD_ALERT = "alert";
    String FIELD_EVENT_DEFINITION_ID = "event_definition_id";

    String FIELD_PRIORITY = "priority";
    String FIELD_EVENT_KEYS = "event_keys";

    @JsonProperty(FIELD_ID)
    String id();

    @JsonProperty(FIELD_STREAMS)
    Set<String> streams();

    @JsonProperty(FIELD_EVENT_TIMESTAMP)
    DateTime timestamp();

    @JsonProperty(FIELD_MESSAGE)
    String message();

    @JsonProperty(FIELD_ALERT)
    boolean alert();

    @JsonProperty(FIELD_EVENT_DEFINITION_ID)
    String eventDefinitionId();

    @JsonProperty(FIELD_PRIORITY)
    Integer priority();

    @JsonProperty(FIELD_EVENT_KEYS)
    List<String> eventKeys();

    @JsonIgnore
    Map<String, Object> rawEvent();
}
