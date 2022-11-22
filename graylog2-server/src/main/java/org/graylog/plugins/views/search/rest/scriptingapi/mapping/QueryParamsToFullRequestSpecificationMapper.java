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

import org.graylog.plugins.views.search.rest.scriptingapi.parsing.MetricParser;
import org.graylog.plugins.views.search.rest.scriptingapi.parsing.TimerangeParser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryParamsToFullRequestSpecificationMapper {

    private final MetricParser metricParser;
    private final TimerangeParser timerangeParser;

    @Inject
    public QueryParamsToFullRequestSpecificationMapper(final TimerangeParser timerangeParser,
                                                       final MetricParser metricParser) {
        this.timerangeParser = timerangeParser;
        this.metricParser = metricParser;
    }

    public SearchRequestSpec simpleQueryParamsToFullRequestSpecification(final String query,
                                                                         final Set<String> streams,
                                                                         final String timerangeKeyword,
                                                                         List<String> groups,
                                                                         List<String> metrics) {
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
                timerangeParser.parseTimeRange(timerangeKeyword),
                groups.stream().map(Grouping::new).collect(Collectors.toList()),
                metrics.stream().map(metricParser::parseMetric).collect(Collectors.toList())
        );
    }


}
