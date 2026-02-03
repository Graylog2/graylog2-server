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
public abstract class Memory {
    @JsonProperty("total")
    public abstract long total();

    @JsonProperty("free")
    public abstract long free();

    @JsonProperty("free_percent")
    public abstract short freePercent();

    @JsonProperty("used")
    public abstract long used();

    @JsonProperty("used_percent")
    public abstract short usedPercent();

    @JsonProperty("actual_free")
    public abstract long actualFree();

    @JsonProperty("actual_used")
    public abstract long actualUsed();

    @JsonCreator
    public static Memory create(@JsonProperty("total") long total,
                                @JsonProperty("free") long free,
                                @JsonProperty("free_percent") short freePercent,
                                @JsonProperty("used") long used,
                                @JsonProperty("used_percent") short usedPercent,
                                @JsonProperty("actual_free") long actualFree,
                                @JsonProperty("actual_used") long actualUsed) {
        return new AutoValue_Memory(total, free, freePercent, used, usedPercent, actualFree, actualUsed);
    }
}
