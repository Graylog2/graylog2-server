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
package org.graylog.plugins.views.search.views.formatting.highlighting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GradientColor implements HighlightingColor {
    static final String TYPE = "gradient";

    @Override
    @JsonProperty
    public String type() {
        return TYPE;
    }

    @JsonProperty
    public abstract String gradient();

    @JsonProperty
    public abstract Number lower();

    @JsonProperty
    public abstract Number upper();

    @JsonCreator
    public static GradientColor create(@JsonProperty("gradient") String gradient,
                                       @JsonProperty("lower") Number lower,
                                       @JsonProperty("upper") Number upper) {
        return new AutoValue_GradientColor(gradient, lower, upper);
    }
}
