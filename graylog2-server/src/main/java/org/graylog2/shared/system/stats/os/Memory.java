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
public abstract class Memory {
    @JsonProperty
    public abstract long total();

    @JsonProperty
    public abstract long free();

    @JsonProperty
    public abstract short freePercent();

    @JsonProperty
    public abstract long used();

    @JsonProperty
    public abstract short usedPercent();

    @JsonProperty
    public abstract long actualFree();

    @JsonProperty
    public abstract long actualUsed();

    public static Memory create(long total,
                                long free,
                                short freePercent,
                                long used,
                                short usedPercent,
                                long actualFree,
                                long actualUsed) {
        return new AutoValue_Memory(total, free, freePercent, used, usedPercent, actualFree, actualUsed);
    }
}
