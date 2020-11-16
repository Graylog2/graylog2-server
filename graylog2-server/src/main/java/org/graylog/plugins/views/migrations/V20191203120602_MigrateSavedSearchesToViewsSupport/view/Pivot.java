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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.Time;

@AutoValue
public abstract class Pivot {
    private static final String TYPE_TIME = "time";

    static final String FIELD_FIELD_NAME = "field";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIG = "config";


    @JsonProperty(FIELD_FIELD_NAME)
    public abstract String field();

    @JsonProperty(FIELD_TYPE)
    public String type() {
        return TYPE_TIME;
    }

    @JsonProperty(FIELD_CONFIG)
    public abstract TimeHistogramConfig config();

    static Builder timeBuilder() {
        return new AutoValue_Pivot.Builder()
                .config(TimeHistogramConfig.create());
    }

    Time toBucketSpec() {
        return Time.create(field(), config().interval().toBucketInterval());
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder field(String field);

        public abstract Builder config(TimeHistogramConfig config);

        public abstract Pivot build();
    }
}
