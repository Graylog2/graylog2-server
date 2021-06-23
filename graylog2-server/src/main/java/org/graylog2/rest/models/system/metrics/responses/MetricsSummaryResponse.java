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
package org.graylog2.rest.models.system.metrics.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class MetricsSummaryResponse {
    @JsonProperty
    public abstract int total();
    @JsonProperty
    public abstract List<Map<String, Object>> metrics();

    @JsonCreator
    public static MetricsSummaryResponse create(@JsonProperty("total") int total, @JsonProperty("metrics") List<Map<String, Object>> metrics) {
        return new AutoValue_MetricsSummaryResponse(total, metrics);
    }

    public static MetricsSummaryResponse create(List<Map<String, Object>> metrics) {
        return new AutoValue_MetricsSummaryResponse(metrics.size(), metrics);
    }
}
