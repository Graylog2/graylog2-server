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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class NodeResponse {
    public abstract String id();

    public abstract String name();

    @Nullable
    public abstract String host();

    public abstract String ip();

    public abstract String diskUsed();

    public abstract String diskTotal();

    public abstract Double diskUsedPercent();

    public abstract Long fileDescriptorMax();

    @JsonCreator
    public static NodeResponse create(@JsonProperty("id") String id,
                                      @JsonProperty("name") String name,
                                      @JsonProperty("host") @Nullable String host,
                                      @JsonProperty("ip") String ip,
                                      @JsonProperty("diskUsed") String diskUsed,
                                      @JsonProperty("diskTotal") String diskTotal,
                                      @JsonProperty("diskUsedPercent") Double diskUsedPercent,
                                      @JsonProperty("fileDescriptorMax") Long fileDescriptorMax) {
        return new AutoValue_NodeResponse(
                id,
                name,
                host,
                ip,
                diskUsed,
                diskTotal,
                diskUsedPercent,
                fileDescriptorMax
        );
    }
}
