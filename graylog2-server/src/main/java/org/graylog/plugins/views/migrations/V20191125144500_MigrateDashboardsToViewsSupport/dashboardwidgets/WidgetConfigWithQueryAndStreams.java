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

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.AggregationWidget;

import java.util.Collections;
import java.util.Optional;

public interface WidgetConfigWithQueryAndStreams extends WidgetConfigWithTimeRange {
    String query();

    Optional<String> streamId();

    default AggregationWidget.Builder createAggregationWidget(String id) {
        final AggregationWidget.Builder viewWidgetBuilder = AggregationWidget.builder()
                .id(id)
                .query(query())
                .timerange(timerange());
        return streamId().map(streamId -> viewWidgetBuilder.streams(Collections.singleton(streamId))).orElse(viewWidgetBuilder);
    }
}
