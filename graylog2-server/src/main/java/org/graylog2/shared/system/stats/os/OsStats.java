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
public abstract class OsStats {
    public static final double[] EMPTY_LOAD = new double[0];

    @JsonProperty
    @SuppressWarnings("mutable")
    public abstract double[] loadAverage();

    @JsonProperty
    public abstract long uptime();

    @JsonProperty
    public abstract Processor processor();

    @JsonProperty
    public abstract Memory memory();

    @JsonProperty
    public abstract Swap swap();

    public static OsStats create(double[] loadAverage,
                                 long uptime,
                                 Processor processor,
                                 Memory memory,
                                 Swap swap) {
        return new AutoValue_OsStats(loadAverage, uptime, processor, memory, swap);
    }
}
