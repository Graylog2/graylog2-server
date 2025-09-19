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
package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class SystemJVMResponse {
    @JsonProperty("free_memory")
    public abstract Map<String, Long> freeMemory();

    @JsonProperty("max_memory")
    public abstract Map<String, Long> maxMemory();

    @JsonProperty("total_memory")
    public abstract Map<String, Long> totalMemory();

    @JsonProperty("used_memory")
    public abstract Map<String, Long> usedMemory();

    @JsonProperty("node_id")
    public abstract String nodeId();

    @JsonProperty("pid")
    public abstract String pid();

    @JsonProperty("info")
    public abstract String info();

    @JsonCreator
    public static SystemJVMResponse create(@JsonProperty("free_memory") Map<String, Long> freeMemory,
                                           @JsonProperty("max_memory") Map<String, Long> maxMemory,
                                           @JsonProperty("total_memory") Map<String, Long> totalMemory,
                                           @JsonProperty("used_memory") Map<String, Long> usedMemory,
                                           @JsonProperty("node_id") String nodeId,
                                           @JsonProperty("pid") String pid,
                                           @JsonProperty("info") String info) {
        return new AutoValue_SystemJVMResponse(freeMemory, maxMemory, totalMemory, usedMemory, nodeId, pid, info);
    }
}
