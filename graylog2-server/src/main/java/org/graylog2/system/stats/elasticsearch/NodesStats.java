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
package org.graylog2.system.stats.elasticsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class NodesStats {
    @JsonProperty
    public abstract int total();

    @JsonProperty
    public abstract int masterOnly();

    @JsonProperty
    public abstract int dataOnly();

    @JsonProperty
    public abstract int masterData();

    @JsonProperty
    public abstract int client();

    public static NodesStats create(int total,
                                   int masterOnly,
                                   int dataOnly,
                                   int masterData,
                                   int client) {
        return new AutoValue_NodesStats(total, masterOnly, dataOnly, masterData, client);
    }
}
