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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class Processor {
    @JsonProperty
    public abstract String model();

    @JsonProperty
    public abstract String vendor();

    @JsonProperty
    public abstract int mhz();

    @JsonProperty
    public abstract int totalCores();

    @JsonProperty
    public abstract int totalSockets();

    @JsonProperty
    public abstract int coresPerSocket();

    @JsonProperty
    public abstract long cacheSize();

    @JsonProperty
    public abstract short sys();

    @JsonProperty
    public abstract short user();

    @JsonProperty
    public abstract short idle();

    @JsonProperty
    public abstract short stolen();

    public static Processor create(String model,
                                   String vendor,
                                   int mhz,
                                   int totalCores,
                                   int totalSockets,
                                   int coresPerSocket,
                                   long cacheSize,
                                   short sys,
                                   short user,
                                   short idle,
                                   short stolen) {
        return new AutoValue_Processor(model, vendor, mhz, totalCores, totalSockets, coresPerSocket, cacheSize,
                sys, user, idle, stolen);
    }
}
