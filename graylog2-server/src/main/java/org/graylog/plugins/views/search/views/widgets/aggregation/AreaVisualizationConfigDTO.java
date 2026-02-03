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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(AreaVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = AreaVisualizationConfigDTO.Builder.class)
public abstract class AreaVisualizationConfigDTO implements VisualizationConfigDTO, XYVisualizationConfig {
    public static final String NAME = "area";
    private static final String FIELD_INTERPOLATION = "interpolation";

    @JsonProperty(FIELD_INTERPOLATION)
    public abstract Interpolation interpolation();

    @JsonProperty(FIELD_AXIS_TYPE)
    public abstract AxisType axisType();

    @JsonProperty(FIELD_AXIS_CONFIG)
    public abstract Optional<AxisConfig> axisConfig();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_INTERPOLATION)
        public abstract Builder interpolation(Interpolation interpolation);

        @JsonProperty(FIELD_AXIS_TYPE)
        public abstract Builder axisType(AxisType axisType);

        @JsonProperty(FIELD_AXIS_CONFIG)
        public abstract Builder axisConfig(@Nullable AxisConfig axisConfig);

        public abstract AreaVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_AreaVisualizationConfigDTO.Builder()
                    .interpolation(Interpolation.defaultValue())
                    .axisType(DEFAULT_AXIS_TYPE);
        }
    }
}
