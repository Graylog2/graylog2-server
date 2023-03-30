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
package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class NodeResponse {
    public abstract String id();

    public abstract String name();

    public abstract String role();

    @Nullable
    public abstract String host();

    public abstract String ip();

    @Nullable
    public abstract String diskUsed();

    @Nullable
    public abstract String diskTotal();

    @Nullable
    public abstract Double diskUsedPercent();

    @Nullable
    public abstract Long fileDescriptorMax();

    @JsonCreator
    public static NodeResponse create(@JsonProperty("id") String id,
                                      @JsonProperty("name") String name,
                                      @JsonProperty("role") String role,
                                      @JsonProperty("host") @Nullable String host,
                                      @JsonProperty("ip") String ip,
                                      @JsonProperty("diskUsed") @Nullable String diskUsed,
                                      @JsonProperty("diskTotal") @Nullable String diskTotal,
                                      @JsonProperty("diskUsedPercent") @Nullable Double diskUsedPercent,
                                      @JsonProperty("fileDescriptorMax") @Nullable Long fileDescriptorMax) {
        return new AutoValue_NodeResponse(
                id,
                name,
                role,
                host,
                ip,
                diskUsed,
                diskTotal,
                diskUsedPercent,
                fileDescriptorMax
        );
    }

    @JsonIgnore
    public boolean hasDiskStatistics() {
        return diskUsed() != null &&
                diskTotal() != null &&
                diskUsedPercent() != null &&
                fileDescriptorMax() != null;
    }
}
