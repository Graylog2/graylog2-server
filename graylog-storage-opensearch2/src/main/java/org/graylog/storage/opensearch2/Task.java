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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.indexer.migration.TaskStatus;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Task(
        @JsonProperty("node") String node,
        @JsonProperty("id") int id,
        @JsonProperty("type") String type,
        @JsonProperty("action") String action,
        @JsonProperty("status") TaskStatus status,
        @JsonProperty("description") String description,
        @JsonProperty("start_time_in_millis") long startTimeInMillis,
        @JsonProperty("running_time_in_nanos") long runningTimeInNanos,
        @JsonProperty("cancellable") boolean cancellable,
        @JsonProperty("cancelled") boolean cancelled,
        @JsonProperty("Headers") Map<String, String> headers
) {
}
