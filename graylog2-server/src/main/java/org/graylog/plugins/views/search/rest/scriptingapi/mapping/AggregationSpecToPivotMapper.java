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

import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Sortable;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AggregationSpecToPivotMapper implements Function<AggregationRequestSpec, Pivot> {

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
    public Pivot apply(final AggregationRequestSpec aggregationSpec) {
        final List<BucketSpec> groups = aggregationSpec.groupings()
                .stream()
                .map(rowGroupCreator)
                .collect(Collectors.toList());

        final Map<Metric, SeriesSpec> series = aggregationSpec.metrics()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Function.identity(), seriesCreator));

        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(false)
                .rowGroups(groups)
                .series(new ArrayList<>(series.values()));

        if (aggregationSpec.hasCustomSort()) {
            final List<SortSpec> newSort = getSortSpecs(series);
            final List<SortSpec> oldSort = getSortSpecs(aggregationSpec.metrics());

            if (!Objects.equals(newSort, oldSort)) {
                throw new IllegalArgumentException("different sorts: " + oldSort + "," + newSort);
            }
            pivotBuilder.sort(newSort);
        }
        return pivotBuilder
                .build();
    }

    private List<SortSpec> getSortSpecs(Map<Metric, SeriesSpec> series) {
        return series.entrySet().stream()
                .filter(e -> e.getKey().sort() != null)
                .map(sortable -> SeriesSort.create(sortable.getValue().literal(), sortable.getKey().sort()))
                .collect(Collectors.toList());
    }

    @Deprecated
    private List<SortSpec> getSortSpecs(final Collection<Metric> groupings) {
        return groupings.stream()
                .filter(sortable -> sortable.sort() != null)
                .map(sortable -> SeriesSort.create(sortable.columnName(), sortable.sort()))
                .collect(Collectors.toList());
    }


}
