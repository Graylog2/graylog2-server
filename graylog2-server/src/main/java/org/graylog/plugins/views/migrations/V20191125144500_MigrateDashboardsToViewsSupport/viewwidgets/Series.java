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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.SeriesSpec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
public abstract class Series {
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_FUNCTION = "function";

    private static final Pattern destructuringPattern = Pattern.compile("(\\w+)\\(([\\w@-]+)?\\)");

    @JsonProperty(FIELD_CONFIG)
    public abstract SeriesConfig config();

    @JsonProperty(FIELD_FUNCTION)
    public abstract String function();

    public static Builder builder() {
        return new AutoValue_Series.Builder()
                .config(SeriesConfig.empty());
    }

    public static Builder buildFromString(String function) {
        return builder().function(function).config(SeriesConfig.empty());
    }

    public static Series create(String function, String field) {
        return buildFromString(function + "(" + field + ")").build();
    }

    public SeriesSpec toSeriesSpec() {
        final Matcher matcher = destructuringPattern.matcher(function());
        if (matcher.matches()) {
            final String functionName = matcher.group(1);
            final String optionalFieldName = matcher.group(2);
            return SeriesSpec.create(functionName, function(), optionalFieldName);
        }

        throw new RuntimeException("Unable to parse function: " + function());
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder config(SeriesConfig config);
        public abstract Builder function(String function);
        public abstract Series build();

    }
}
