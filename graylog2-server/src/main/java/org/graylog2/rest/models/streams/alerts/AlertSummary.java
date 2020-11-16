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
package org.graylog2.rest.models.streams.alerts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class AlertSummary {
    @JsonProperty
    public abstract String id();

    @JsonProperty("condition_id")
    public abstract String conditionId();

    @JsonProperty("stream_id")
    public abstract String streamId();

    @JsonProperty
    public abstract String description();

    @JsonProperty("condition_parameters")
    public abstract Map<String, Object> conditionParameters();

    @JsonProperty("triggered_at")
    public abstract DateTime triggeredAt();

    @JsonProperty("resolved_at")
    @Nullable
    public abstract DateTime resolvedAt();

    @JsonProperty("is_interval")
    public abstract boolean isInterval();

    @JsonCreator
    public static AlertSummary create(@JsonProperty("id") String id,
                                      @JsonProperty("condition_id") String conditionId,
                                      @JsonProperty("stream_id") String streamId,
                                      @JsonProperty("description") String description,
                                      @JsonProperty("condition_parameters") Map<String, Object> conditionParameters,
                                      @JsonProperty("triggered_at") DateTime triggeredAt,
                                      @JsonProperty("resolved_at") DateTime resolvedAt,
                                      @JsonProperty("is_interval") boolean isInterval) {
        return new AutoValue_AlertSummary(id, conditionId, streamId, description, conditionParameters, triggeredAt, resolvedAt, isInterval);
    }
}
