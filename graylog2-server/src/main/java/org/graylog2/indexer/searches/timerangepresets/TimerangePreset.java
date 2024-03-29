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
package org.graylog2.indexer.searches.timerangepresets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.UUID;

public record TimerangePreset(
        @JsonProperty(value = "id", required = true) String id,
        @JsonProperty(value = "timerange", required = true) TimeRange timeRange,
        @JsonProperty(value = "description", required = true) String description) {

    @JsonCreator
    public TimerangePreset(@JsonProperty(value = "id", required = true) String id,
                           @JsonProperty(value = "timerange", required = true) TimeRange timeRange,
                           @JsonProperty(value = "description", required = true) String description) {
        this.id = id;
        this.timeRange = timeRange;
        this.description = description;
    }

    public TimerangePreset(TimeRange timeRange, String description) {
        this(UUID.randomUUID().toString(), timeRange, description);
    }
}
