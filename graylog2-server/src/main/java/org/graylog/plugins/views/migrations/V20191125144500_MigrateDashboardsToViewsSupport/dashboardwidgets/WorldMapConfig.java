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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class WorldMapConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    private static final String MAP_VISUALIZATION = "map";

    public abstract String field();
    public abstract Optional<String> streamId();

    private Series series() {
        return countSeries();
    }

    private Pivot fieldPivot() {
        return valuesPivotForField(field(), 15);
    }

    @Override
    public Set<ViewWidget> toViewWidgets(Widget widget, RandomUUIDProvider randomUUIDProvider) {
        return Collections.singleton(createAggregationWidget(randomUUIDProvider.get())
                .config(AggregationConfig.builder()
                        .rowPivots(Collections.singletonList(fieldPivot()))
                        .series(Collections.singletonList(series()))
                        .visualization(MAP_VISUALIZATION)
                        .build())
                .build());
    }

    @JsonCreator
    static WorldMapConfig create(
            @JsonProperty("field") String field,
            @JsonProperty("stream_id") @Nullable String streamId,
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_WorldMapConfig(
                timerange,
                Strings.nullToEmpty(query),
                field,
                Optional.ofNullable(streamId)
        );
    }
}
