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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.NumberVisualizationConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@WithBeanGetter
public abstract class AggregationWidget implements ViewWidget {
    private static final String TYPE_AGGREGATION = "aggregation";

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    abstract String type();

    @JsonProperty(FIELD_FILTER)
    @Nullable
    abstract String filter();

    @JsonProperty(FIELD_TIMERANGE)
    abstract TimeRange timerange();

    @JsonProperty(FIELD_QUERY)
    abstract ElasticsearchQueryString query();

    @JsonProperty(FIELD_STREAMS)
    abstract Set<String> streams();

    @JsonProperty(FIELD_CONFIG)
    public abstract AggregationConfig config();

    public static Builder builder() {
        return new AutoValue_AggregationWidget.Builder()
                .type(TYPE_AGGREGATION)
                .streams(Collections.emptySet());
    }

    public Set<SearchType> toSearchTypes(RandomUUIDProvider randomUUIDProvider) {
        final Pivot.Builder chartBuilder = Pivot.builder()
                .id(randomUUIDProvider.get())
                .name("chart")
                .query(query())
                .streams(streams())
                .timerange(timerange())
                .rollup(config().rollup())
                .rowGroups(config().rowPivots().stream().map(pivot -> pivot.toBucketSpec()).collect(Collectors.toList()))
                .columnGroups(config().columnPivots().stream().map(pivot -> pivot.toBucketSpec()).collect(Collectors.toList()))
                .series(config().series().stream().map(series -> series.toSeriesSpec()).collect(Collectors.toList()))
                .sort(config().sort().stream().map(sort -> sort.toSortSpec()).collect(Collectors.toList()));

        if (config().visualization().equals("numeric") && config().visualizationConfig()
                .map(visualizationConfig -> ((NumberVisualizationConfig) visualizationConfig).trend())
                .orElse(false)) {
            final Pivot chart = chartBuilder
                    .build();
            final Pivot trend = chartBuilder
                    .id(randomUUIDProvider.get())
                    .name("trend")
                    .timerange(OffsetRange.ofSearchTypeId(chart.id()))
                    .build();

            return ImmutableSet.of(chart, trend);
        }
        return Collections.singleton(chartBuilder.build());
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_FILTER)
        @Nullable
        public abstract Builder filter(String filter);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        abstract Builder query(ElasticsearchQueryString query);
        public Builder query(String query) {
            return query(ElasticsearchQueryString.create(query));
        }

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(AggregationConfig config);

        public abstract AggregationWidget build();
    }
}
