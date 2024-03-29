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
package org.graylog2.cluster;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import jakarta.validation.constraints.NotEmpty;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ClusterConfigChangedEvent {
    @JsonProperty
    public abstract DateTime date();

    @JsonProperty
    @NotEmpty
    public abstract String nodeId();

    @JsonProperty
    @NotEmpty
    public abstract String type();

    @JsonCreator
    public static ClusterConfigChangedEvent create(@JsonProperty("date") DateTime date,
                                                   @JsonProperty("node_id") @NotEmpty String nodeId,
                                                   @JsonProperty("type") @NotEmpty String type) {
        return new AutoValue_ClusterConfigChangedEvent(date, nodeId, type);
    }
}
