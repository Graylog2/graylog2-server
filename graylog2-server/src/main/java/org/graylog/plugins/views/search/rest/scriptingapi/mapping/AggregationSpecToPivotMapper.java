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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
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

        final List<ImmutablePair<Metric, SeriesSpec>> series = aggregationSpec.metrics()
                .stream()
                .filter(Objects::nonNull)
                .map(m -> ImmutablePair.of(m, seriesCreator.apply(m)))
                .collect(Collectors.toList());

        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(false)
                .rowGroups(groups)
                .series(series.stream().map(ImmutablePair::getValue).collect(Collectors.toList()));

        if (aggregationSpec.hasCustomSort()) {
            final List<SortSpec> newSort = getSortSpecs(series);
            pivotBuilder.sort(newSort);
        }
        return pivotBuilder
                .build();
    }

    private List<SortSpec> getSortSpecs(List<ImmutablePair<Metric, SeriesSpec>> series) {
        return series.stream()
                .filter(e -> e.getKey().sort() != null)
                .map(sortable -> SeriesSort.create(sortable.getValue().literal(), sortable.getKey().sort()))
                .collect(Collectors.toList());
    }
}
