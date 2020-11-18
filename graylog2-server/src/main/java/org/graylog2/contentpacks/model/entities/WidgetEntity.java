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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;
import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog.plugins.views.search.views.WidgetDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.PivotSortConfig;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.SortConfigDTO;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = WidgetEntity.Builder.class)
@WithBeanGetter
public abstract class WidgetEntity implements NativeEntityConverter<WidgetDTO> {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_FILTER = "filter";
    public static final String FIELD_CONFIG = "config";
    public static final String FIELD_TIMERANGE = "timerange";
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_FILTER)
    @Nullable
    public abstract String filter();

    @JsonProperty(FIELD_TIMERANGE)
    public abstract Optional<TimeRange> timerange();

    @JsonProperty(FIELD_QUERY)
    public abstract Optional<BackendQuery> query();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_CONFIG)
    public abstract WidgetConfigDTO config();

    public static Builder builder() {
        return Builder.builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_FILTER)
        public abstract Builder filter(@Nullable String filter);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = FIELD_TYPE,
                visible = true)
        public abstract Builder config(WidgetConfigDTO config);

        public abstract WidgetEntity build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_WidgetEntity.Builder().streams(Collections.emptySet());
        }
    }

    @Override
    public WidgetDTO toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        final WidgetDTO.Builder widgetBuilder = WidgetDTO.builder()
                .config(this.config())
                .filter(this.filter())
                .id(this.id())
                .streams(this.streams().stream()
                        .map(id -> EntityDescriptor.create(id, ModelTypes.STREAM_V1))
                        .map(nativeEntities::get)
                        .map(object -> {
                            if (object == null) {
                                throw new ContentPackException("Missing Stream for widget entity");
                            } else if (object instanceof Stream) {
                                Stream stream = (Stream) object;
                                return stream.getId();
                            } else {
                                throw new ContentPackException(
                                        "Invalid type for stream Stream for event definition: " + object.getClass());
                            }
                        }).collect(Collectors.toSet()))
                .type(this.type());
        if (this.query().isPresent()) {
            widgetBuilder.query(this.query().get());
        }
        if (this.timerange().isPresent()) {
            widgetBuilder.timerange(this.timerange().get());
        }
        return widgetBuilder.build();
    }

    public List<SearchTypeEntity> createSearchTypeEntity() {
        if (! type().matches(AggregationConfigDTO.NAME)) {
            return ImmutableList.of();
        }
        AggregationConfigDTO config = (AggregationConfigDTO) config();
        final PivotEntity.Builder pivotBuilder = PivotEntity.builder()
                .name("chart")
                .streams(streams())
                .rollup(true)
                .sort(toSortSpec(config))
                .rowGroups(toRowGroups(config))
                .series(toSeriesSpecs(config))
                .id(UUID.randomUUID().toString());
        query().ifPresent(pivotBuilder::query);
        timerange().ifPresent(pivotBuilder::timerange);

        if (config.visualization().matches("numeric")) {
            final PivotEntity chart = pivotBuilder.build();
            final PivotEntity trend = pivotBuilder
                    .id(UUID.randomUUID().toString())
                    .name("trend")
                    .timerange(OffsetRange.Builder.builder()
                            .source("search_type")
                            .id(chart.id())
                            .build())
                    .build();
            return ImmutableList.of(chart, trend);
        }

        return ImmutableList.of(pivotBuilder.build());
    }

    private List<SortSpec> toSortSpec(AggregationConfigDTO config) {
        return config.sort().stream().map(sortConfig -> {
           final PivotSortConfig pivotSortConfig = (PivotSortConfig) sortConfig;
           final SortSpec.Direction dir = pivotSortConfig.direction().equals(SortConfigDTO.Direction.Ascending)
                    ? SortSpec.Direction.Ascending
                    : SortSpec.Direction.Descending;
           return PivotSort.create(PivotSort.Type, pivotSortConfig.field(), dir);
        }).collect(Collectors.toList());

    }

    private List<BucketSpec> toRowGroups(AggregationConfigDTO config) {
        return config.rowPivots().stream().map(rowPivot -> {
            if (rowPivot.type().matches("time")) {
                return Time.builder()
                        .field(rowPivot.field())
                        .interval(AutoInterval.create()).build();
            } else {
                return Values.builder()
                        .field(rowPivot.field())
                        .build();
            }
        }).collect(Collectors.toList());
    }

    private List<SeriesSpec> toSeriesSpecs(AggregationConfigDTO config) {
        return config.series().stream().map(seriesDTO -> {
            String function = seriesDTO.function();
            Pattern pattern = Pattern.compile("\\((.*?)\\)");
            Matcher matcher = pattern.matcher(function);
            String field = "";
            if (matcher.find()) {
                field = matcher.group(1);
            }
            if (function.startsWith("card")) {
                return Cardinality.builder().field(field).id(function).build();
            }
            if (function.startsWith("avg")) {
                return Average.builder().field(field).id(function).build();
            }
            if (function.startsWith("max")) {
                return Max.builder().field(field).id(function).build();
            }
            if (function.startsWith("min")) {
                return Min.builder().field(field).id(function).build();
            }
            if (function.startsWith("sum")) {
                return Sum.builder().field(field).id(function).build();
            }
            if (function.startsWith("variance")) {
                return Variance.builder().field(field).id(function).build();
            }
            if (function.startsWith("stddev")) {
                return StdDev.builder().field(field).id(function).build();
            }
            if (function.startsWith("count")) {
                final Count.Builder countBuilder = Count.builder().id(function);
                if (!field.isEmpty()) {
                    countBuilder.field(field);
                }
                return countBuilder.build();
            }
            throw new IllegalArgumentException(
                    "The provided entity does not have a valid function type: " + function);
        }).collect(Collectors.toList());
    }
}
