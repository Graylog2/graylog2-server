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

/**
 * This class duplicates the original opensearch GetTaskResponse with one significant exception - it can read the
 * error of the task, which is missing in the original response
 * TODO: add link to an feature request issue in opensearch java client
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GetTaskResponse(@JsonProperty("completed") boolean completed, @JsonProperty("task") Task task,
                              @JsonProperty("error") TaskError error) {
}
