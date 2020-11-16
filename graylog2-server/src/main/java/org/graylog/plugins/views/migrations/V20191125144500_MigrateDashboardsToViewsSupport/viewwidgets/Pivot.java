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
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.BucketSpec;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Time;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Values;

@AutoValue
public abstract class Pivot {
    private static final String TYPE_TIME = "time";
    private static final String TYPE_VALUES = "values";

    static final String FIELD_FIELD_NAME = "field";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIG = "config";


    @JsonProperty(FIELD_FIELD_NAME)
    public abstract String field();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_CONFIG)
    public abstract PivotConfig config();

    public static Builder timeBuilder() {
        return new AutoValue_Pivot.Builder()
                .type(TYPE_TIME);
    }

    public static Builder valuesBuilder() {
        return new AutoValue_Pivot.Builder()
                .type(TYPE_VALUES);
    }

    public BucketSpec toBucketSpec() {
        switch (type()) {
            case TYPE_TIME:
                final TimeHistogramConfig timeConfig = (TimeHistogramConfig)config();
                return Time.create(field(), timeConfig.interval().toBucketInterval());
            case TYPE_VALUES:
                final ValueConfig valueConfig = (ValueConfig)config();
                return Values.create(field(), valueConfig.limit());
        }

        throw new RuntimeException("Invalid pivot type when creating bucket spec: " + type());
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder field(String field);

        public abstract Builder config(PivotConfig config);

        public abstract Builder type(String type);

        public abstract Pivot build();
    }
}
