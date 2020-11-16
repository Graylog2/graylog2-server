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
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.BucketInterval;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class AutoInterval implements Interval {
    public static final String type = "auto";
    private static final String FIELD_SCALING = "scaling";

    @JsonProperty
    public abstract String type();

    @JsonProperty(FIELD_SCALING)
    public abstract Optional<Double> scaling();

    @Override
    public BucketInterval toBucketInterval() {
        return org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.AutoInterval.create();
    }

    private static Builder builder() { return new AutoValue_AutoInterval.Builder().type(type); };

    public static AutoInterval create() {
        return AutoInterval.builder().build();
    }

    public static AutoInterval create(Double scaling) {
        return AutoInterval.builder().scaling(scaling).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String type);
        public abstract Builder scaling(@Nullable Double scaling);

        public abstract AutoInterval build();
    }
}

