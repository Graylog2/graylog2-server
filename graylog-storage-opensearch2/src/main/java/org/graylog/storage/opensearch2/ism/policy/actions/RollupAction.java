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
package org.graylog.storage.opensearch2.ism.policy.actions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

public record RollupAction(IsmRollup ismRollup) implements WrappedAction {

    @Override
    public Type getType() {
        return Type.ROLLUP;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IsmRollup(String targetIndex, String description, int pageSize,
                            List<Dimension> dimensions, List<Metric> metrics) {

        @JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = DateHistogram.class, name = "date_histogram"),
        })
        public interface Dimension {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record DateHistogram(String sourceField, String fixedInterval, String timezone) implements Dimension {}

        public record Metric(String sourceField, List<AggregationMetric> metrics) {}

        @JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = AvgMetric.class, name = "avg"),
        })
        public interface AggregationMetric {}

        public record AvgMetric() implements AggregationMetric {}

    }
}
