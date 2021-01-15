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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = ViewWidgetPosition.Builder.class)
@WithBeanGetter
public abstract class ViewWidgetPosition {
    @JsonProperty("col")
    abstract Position col();

    @JsonProperty("row")
    abstract Position row();

    @JsonProperty("height")
    abstract Position height();

    @JsonProperty("width")
    abstract Position width();

    public static Builder builder() { return new AutoValue_ViewWidgetPosition.Builder(); }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder col(Position col);

        public abstract Builder row(Position row);

        public abstract Builder height(Position height);

        public abstract Builder width(Position width);

        public abstract ViewWidgetPosition build();
    }
}
