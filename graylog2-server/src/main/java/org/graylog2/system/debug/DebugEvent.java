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
package org.graylog2.system.debug;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class DebugEvent {
    @JsonProperty
    public abstract String nodeId();

    @JsonProperty
    public abstract DateTime date();

    @JsonProperty
    public abstract String text();

    @JsonCreator
    public static DebugEvent create(@JsonProperty("node_id") String nodeId,
                                    @JsonProperty("date") DateTime date,
                                    @JsonProperty("text") String text) {
        return new AutoValue_DebugEvent(nodeId, date, text);
    }

    public static DebugEvent create(String nodeId, String text) {
        return create(nodeId, DateTime.now(DateTimeZone.UTC), text);
    }
}
