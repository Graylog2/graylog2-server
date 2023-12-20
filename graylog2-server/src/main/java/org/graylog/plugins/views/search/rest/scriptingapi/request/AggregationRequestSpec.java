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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record AggregationRequestSpec(@JsonProperty("query") String queryString,
                                     @JsonProperty("streams") Set<String> streams,
                                     @JsonProperty("timerange") TimeRange timerange,
                                     @JsonProperty("group_by") @Valid @NotEmpty List<Grouping> groupings,
                                     @JsonProperty("metrics") @Valid @NotEmpty List<Metric> metrics) implements SearchRequestSpec {


    public AggregationRequestSpec {
        if (Strings.isNullOrEmpty(queryString)) {
            queryString = DEFAULT_QUERY_STRING;
        }
        if (timerange == null) {
            timerange = DEFAULT_TIMERANGE;
        }
        if (streams == null) {
            streams = Set.of();
        }
    }

    public boolean hasCustomSort() {
        return metrics().stream().anyMatch(m -> m.sort() != null);
    }

    @JsonIgnore
    public List<ResponseSchemaEntry> getSchema() {
        final Stream<ResponseSchemaEntry> groupings = groupings().stream()
                .map(gr -> ResponseSchemaEntry.groupBy(gr.requestedField().toString()));

        final Stream<ResponseSchemaEntry> metrics = metrics().stream()
                .map(metric -> ResponseSchemaEntry.metric(metric.functionName(), metric.fieldName()));

        return Stream.concat(groupings, metrics).collect(Collectors.toList());
    }
}
