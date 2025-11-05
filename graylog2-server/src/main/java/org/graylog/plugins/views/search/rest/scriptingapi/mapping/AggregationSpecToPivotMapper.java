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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import jakarta.inject.Inject;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.streams.StreamService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval.DEFAULT_SCALINGFACTOR;
import static org.graylog2.indexer.fieldtypes.FieldTypeMapper.DATE_TYPE;

public class AggregationSpecToPivotMapper implements BiFunction<AggregationRequestSpec, SearchUser, Pivot> {

    public static final String PIVOT_ID = "scripting_api_temporary_pivot";

    private final GroupingToBucketSpecMapper rowGroupCreator;
    private final MetricToSeriesSpecMapper seriesCreator;
    private final MappedFieldTypesService mappedFieldTypesService;
    private final Function<Collection<String>, Stream<String>> streamCategoryMapper;

    @Inject
    public AggregationSpecToPivotMapper(final GroupingToBucketSpecMapper rowGroupCreator,
                                        final MetricToSeriesSpecMapper seriesCreator,
                                        final MappedFieldTypesService mappedFieldTypesService,
                                        final StreamService streamService) {
        this.rowGroupCreator = rowGroupCreator;
        this.seriesCreator = seriesCreator;
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.streamCategoryMapper = streamService::mapCategoriesToIds;
    }

    private Map<String, FieldTypes.Type> fieldTypesByStreamIds(final AggregationRequestSpec aggregationSpec, final SearchUser searchUser) {
        final Set<String> allowedStreams = searchUser.streams().loadAllMessageStreams();
        Set<String> requestedStreams = new HashSet<>(aggregationSpec.streams());
        if(!aggregationSpec.streamCategories().isEmpty()) {
            requestedStreams.addAll(streamCategoryMapper.apply(aggregationSpec.streamCategories()).toList());
        }
        final var filteredStreams = requestedStreams.isEmpty() ?  allowedStreams : requestedStreams.stream().filter(allowedStreams::contains).collect(Collectors.toSet());
        return mappedFieldTypesService.fieldTypesByStreamIds(filteredStreams, RelativeRange.allTime()).stream().collect(Collectors.toMap(MappedFieldTypeDTO::name, MappedFieldTypeDTO::type));
    }

    private Grouping addAutoInterval(Grouping grouping) {
        return new Grouping(grouping.requestedField().name(), Optional.empty(), Optional.empty(), Optional.of(DEFAULT_SCALINGFACTOR));
    }

    private boolean isDateType(final String fieldName, final Map<String, FieldTypes.Type> fields) {
        return fields.containsKey(fieldName) && fields.get(fieldName).equals(DATE_TYPE);
    }

    private boolean noBucketingParameterSetOn(Grouping grouping) {
        return grouping.timeunit().isEmpty() && grouping.scaling().isEmpty();
    }

    @Override
    public Pivot apply(final AggregationRequestSpec aggregationSpec, final SearchUser searchUser) {
        final var fields = fieldTypesByStreamIds(aggregationSpec, searchUser);

        final List<BucketSpec> groups = aggregationSpec.groupings()
                .stream()
                .map(grouping -> noBucketingParameterSetOn(grouping) && isDateType(grouping.requestedField().name(), fields) ? addAutoInterval(grouping) : grouping)
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
