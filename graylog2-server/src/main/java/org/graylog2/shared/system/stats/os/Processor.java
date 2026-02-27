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
package org.graylog2.shared.system.stats.os;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class Processor {
    @JsonProperty("model")
    public abstract String model();

    @JsonProperty("vendor")
    public abstract String vendor();

    @JsonProperty("mhz")
    public abstract int mhz();

    @JsonProperty("total_cores")
    public abstract int totalCores();

    @JsonProperty("total_sockets")
    public abstract int totalSockets();

    @JsonProperty("cores_per_socket")
    public abstract int coresPerSocket();

    @JsonProperty("cache_size")
    public abstract long cacheSize();

    @JsonProperty("sys")
    public abstract short sys();

    @JsonProperty("user")
    public abstract short user();

    @JsonProperty("idle")
    public abstract short idle();

    @JsonProperty("stolen")
    public abstract short stolen();

    @JsonCreator
    public static Processor create(@JsonProperty("model") String model,
                                   @JsonProperty("vendor") String vendor,
                                   @JsonProperty("mhz") int mhz,
                                   @JsonProperty("total_cores") int totalCores,
                                   @JsonProperty("total_sockets") int totalSockets,
                                   @JsonProperty("cores_per_socket") int coresPerSocket,
                                   @JsonProperty("cache_size") long cacheSize,
                                   @JsonProperty("sys") short sys,
                                   @JsonProperty("user") short user,
                                   @JsonProperty("idle") short idle,
                                   @JsonProperty("stolen") short stolen) {
        return new AutoValue_Processor(model, vendor, mhz, totalCores, totalSockets, coresPerSocket, cacheSize,
                sys, user, idle, stolen);
    }
}
