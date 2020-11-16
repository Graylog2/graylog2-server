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

import java.util.List;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class AlertConditionListSummary {
    @JsonProperty("total")
    public abstract long total();

    @JsonProperty("conditions")
    public abstract List<AlertConditionSummary> conditions();

    @JsonCreator
    public static AlertConditionListSummary create(@JsonProperty("total") long total,
                                                   @JsonProperty("conditions") List<AlertConditionSummary> conditions) {
        return new AutoValue_AlertConditionListSummary(total, conditions);
    }

    public static AlertConditionListSummary create(@JsonProperty("conditions") List<AlertConditionSummary> conditions) {
        return new AutoValue_AlertConditionListSummary(conditions.size(), conditions);
    }
}
