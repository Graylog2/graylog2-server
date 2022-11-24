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

import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Sortable;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AggregationSpecToPivotMapper implements Function<SearchRequestSpec, Pivot> {

    public static final String PIVOT_ID = "scripting_api_temporary_pivot";

    private final GroupingToBucketSpecMapper rowGroupCreator;
    private final MetricToSeriesSpecMapper seriesCreator;

    @Inject
    public AggregationSpecToPivotMapper(final GroupingToBucketSpecMapper rowGroupCreator,
                                        final MetricToSeriesSpecMapper seriesCreator) {
        this.rowGroupCreator = rowGroupCreator;
        this.seriesCreator = seriesCreator;
    }

    @Override
    public Pivot apply(final SearchRequestSpec aggregationSpec) {
        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(false)
                .rowGroups(aggregationSpec.groupings()
                        .stream()
                        .map(rowGroupCreator)
                        .collect(Collectors.toList())
                ).series(aggregationSpec.metrics()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(seriesCreator)
                        .collect(Collectors.toList())
                );
        if (aggregationSpec.hasCustomSort()) {
            pivotBuilder.sort(getSortSpecs(aggregationSpec.metrics()));
        }
        return pivotBuilder
                .build();
    }

    private List<SortSpec> getSortSpecs(final Collection<? extends Sortable> groupings) {
        return groupings.stream()
                .filter(sortable -> sortable.sort() != null)
                .map(sortable -> {
                    if (sortable instanceof Metric) {
                        return SeriesSort.create(sortable.sortColumnName(), sortable.sort());
                    }
                    return PivotSort.create(sortable.sortColumnName(), sortable.sort());
                })
                .collect(Collectors.toList());
    }


}
