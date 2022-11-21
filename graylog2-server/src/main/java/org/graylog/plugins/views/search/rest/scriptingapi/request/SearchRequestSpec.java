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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.rest.scriptingapi.parsing.TimerangeParser;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record SearchRequestSpec(@JsonProperty("query") String queryString,
                                @JsonProperty("streams") Set<String> streams,
                                @JsonProperty("timerange") TimeRange timerange,
                                @JsonProperty("group_by") @Valid @NotEmpty List<Grouping> groupings,
                                @JsonProperty("metrics") @Valid @NotEmpty List<Metric> metrics) {

    public static final RelativeRange DEFAULT_TIMERANGE = RelativeRange.create(24 * 60 * 60);

    public SearchRequestSpec {
        if (Strings.isNullOrEmpty(queryString)) {
            queryString = "*";
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

    public static SearchRequestSpec fromSimpleParams(String query, Set<String> streams, String timerangeKeyword, List<String> groups, List<String> metrics) {
        if (groups == null || groups.isEmpty()) {
            throw new BadRequestException("At least one grouping has to be provided!");
        }
        if (metrics == null || metrics.isEmpty()) {
            metrics = List.of("count:");
        }
        if (!metrics.stream().allMatch(m -> m.contains(":"))) {
            throw new BadRequestException("All metrics need to be defined as \"function\":\"field_name\"");
        }

        return new SearchRequestSpec(
                query,
                streams,
                TimerangeParser.parseTimeRange(timerangeKeyword),
                groups.stream().map(Grouping::new).collect(Collectors.toList()),
                metrics.stream().map(Metric::parseMetric).collect(Collectors.toList())
        );
    }
}
