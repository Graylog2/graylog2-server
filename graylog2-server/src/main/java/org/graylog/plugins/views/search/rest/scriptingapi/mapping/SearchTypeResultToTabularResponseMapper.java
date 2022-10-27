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

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseData;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchTypeResultToTabularResponseMapper {

    public TabularResponse mapToResponse(final SearchRequestSpec searchRequestSpec,
                                         final PivotResult pivotResult) {
        final AggregationSpec aggregationSpec = searchRequestSpec.aggregationSpec();
        final int numGroupings = aggregationSpec.groupings().size();
        final int numMetrics = aggregationSpec.metrics().size();
        final int numColumns = numGroupings + numMetrics;
        List<ResponseSchemaEntry> schema = new ArrayList<>(numColumns);
        aggregationSpec.groupings().forEach(gr -> schema.add(new ResponseSchemaEntry("Grouping on : " + gr.fieldName(), "string")));
        aggregationSpec.metrics().forEach(metric -> schema.add(new ResponseSchemaEntry("Metric : " + metric.functionName(),
                Latest.NAME.equals(metric.functionName()) ? "string" : "numeric")));
        return new TabularResponse(
                schema,
                new ResponseData(
                        pivotResult.rows()
                                .stream()
                                .map(pivRow -> {
                                    List<Object> row = new ArrayList<>(numColumns);
                                    final ImmutableList<String> resultGroupings = pivRow.key();
                                    row.addAll(resultGroupings);
                                    for (int i = 0; i < numGroupings - resultGroupings.size(); i++) {
                                        row.add("-"); //sometimes pivotRow does not have enough keys - empty value!
                                    }
                                    final List<Object> resultMetrics = pivRow.values().stream().map(PivotResult.Value::value).toList();
                                    row.addAll(resultMetrics);
                                    return row;
                                })
                                .collect(Collectors.toList())
                )
        );

    }
}
