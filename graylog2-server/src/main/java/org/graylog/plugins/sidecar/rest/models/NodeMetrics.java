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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class NodeMetrics {
    @JsonProperty("disks_75")
    @Nullable
    public abstract List<String> disks75();

    @JsonProperty("cpu_idle")
    @Nullable
    public abstract Float cpuIdle();

    @JsonProperty("load_1")
    @Nullable
    public abstract Float load1();

    @JsonCreator
    public static NodeMetrics create(@JsonProperty("disks_75") @Nullable List<String> disks75,
                                     @JsonProperty("cpu_idle") @Nullable Float cpuIdle,
                                     @JsonProperty("load_1") @Nullable Float load1) {
        return new AutoValue_NodeMetrics(disks75, cpuIdle, load1);
    }
}
