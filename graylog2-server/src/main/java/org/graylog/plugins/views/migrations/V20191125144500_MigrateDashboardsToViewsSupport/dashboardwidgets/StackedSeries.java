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

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackedSeries {
    public abstract String query();
    public abstract String field();
    public abstract String statisticalFunction();

    @JsonCreator
    public static StackedSeries create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("field") String field,
            @JsonProperty("statistical_function") String statisticalFunction
    ) {
        final String cleanedQuery = Strings.nullToEmpty(query).trim();
        return new AutoValue_StackedSeries(cleanedQuery.equals("") ? "*" : cleanedQuery, field, statisticalFunction);
    }
}
