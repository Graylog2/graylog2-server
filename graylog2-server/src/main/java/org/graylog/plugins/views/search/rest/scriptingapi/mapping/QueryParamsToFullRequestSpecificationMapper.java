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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.apache.commons.collections4.CollectionUtils;
import org.graylog.plugins.views.search.rest.scriptingapi.parsing.TimerangeParser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryParamsToFullRequestSpecificationMapper {

    private final TimerangeParser timerangeParser;

    @Inject
    public QueryParamsToFullRequestSpecificationMapper(final TimerangeParser timerangeParser) {
        this.timerangeParser = timerangeParser;
    }

    public MessagesRequestSpec simpleQueryParamsToFullRequestSpecification(final String query,
                                                                           final Set<String> streams,
                                                                           final String timerangeKeyword,
                                                                           final List<String> fields,
                                                                           final String sort,
                                                                           final SortSpec.Direction sortOrder,
                                                                           final int from,
                                                                           final int size) {

        return new MessagesRequestSpec(query,
                streams,
                timerangeParser.parseTimeRange(timerangeKeyword),
                sort,
                sortOrder,
                from,
                size,
                fields);
    }

    public AggregationRequestSpec simpleQueryParamsToFullRequestSpecification(final String query,
                                                                              final Set<String> streams,
                                                                              final String timerangeKeyword,
                                                                              List<String> groups,
                                                                              List<String> metrics) {
        if (CollectionUtils.isEmpty(groups)) {
            throw new IllegalArgumentException("At least one grouping has to be provided!");
        }
        if (CollectionUtils.isEmpty(metrics)) {
            metrics = List.of("count:");
        }
        if (!metrics.stream().allMatch(m -> m.contains(":") || "count".equals(m))) {
            throw new IllegalArgumentException("All metrics need to be defined as \"function\":\"field_name\"");
        }
        if (metrics.stream().anyMatch(m -> m.startsWith("percentile:"))) {
            throw new IllegalArgumentException("Percentile metric cannot be used in simplified query format. Please use POST request instead, specifying configuration for percentile metric");
        }

        return new AggregationRequestSpec(
                query,
                streams,
                timerangeParser.parseTimeRange(timerangeKeyword),
                groups.stream().map(Grouping::new).collect(Collectors.toList()),
                metrics.stream().map(Metric::fromStringRepresentation).flatMap(Optional::stream).collect(Collectors.toList())
        );
    }


}
