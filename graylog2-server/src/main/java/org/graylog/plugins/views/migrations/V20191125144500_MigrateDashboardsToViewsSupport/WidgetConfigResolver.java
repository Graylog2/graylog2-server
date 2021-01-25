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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.FieldChartConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.QuickValuesConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.QuickValuesHistogramConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.SearchResultChartConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.SearchResultCountConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.StackedChartConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.StatsCountConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.UnknownWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.WidgetConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.WorldMapConfig;

import java.io.IOException;
import java.util.Locale;

public class WidgetConfigResolver implements TypeIdResolver {
    private JavaType superType;

    @Override
    public void init(JavaType baseType) {
        this.superType = baseType;
    }

    private Class<? extends WidgetConfig> resolveTypeId(String id) {
        switch (id.toUpperCase(Locale.ENGLISH)) {
            case "FIELD_CHART": return FieldChartConfig.class;
            case "SEARCH_RESULT_CHART": return SearchResultChartConfig.class;
            case "SEARCH_RESULT_COUNT": return SearchResultCountConfig.class;
            case "STREAM_SEARCH_RESULT_COUNT": return SearchResultCountConfig.class;
            case "STACKED_CHART": return StackedChartConfig.class;
            case "STATS_COUNT": return StatsCountConfig.class;
            case "QUICKVALUES": return QuickValuesConfig.class;
            case "QUICKVALUES_HISTOGRAM": return QuickValuesHistogramConfig.class;
            case "ORG.GRAYLOG.PLUGINS.MAP.WIDGET.STRATEGY.MAPWIDGETSTRATEGY": return WorldMapConfig.class;

            default: return UnknownWidget.class;
        }
    }
    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        return context.constructSpecializedType(superType, resolveTypeId(id));
    }

    @Override
    public String idFromValue(Object value) {
        throw new IllegalStateException("This type resolver is meant for deserialization only!");
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        throw new IllegalStateException("This type resolver is meant for deserialization only!");
    }

    @Override
    public String idFromBaseType() {
        throw new IllegalStateException("This type resolver is meant for deserialization only!");
    }

    @Override
    public String getDescForKnownTypeIds() {
        throw new IllegalStateException("This type resolver is meant for deserialization only!");
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        throw new IllegalStateException("This type resolver is meant for deserialization only!");
    }
}
