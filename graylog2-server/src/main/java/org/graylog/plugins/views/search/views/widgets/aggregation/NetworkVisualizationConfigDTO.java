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

@AutoValue
@JsonTypeName(NetworkVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = NetworkVisualizationConfigDTO.Builder.class)
public abstract class NetworkVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "network";
    private static final String FIELD_COLOR_SCALE = "color_scale";
    private static final String FIELD_REVERSE_SCALE = "reverse_scale";

    @JsonProperty(FIELD_COLOR_SCALE)
    public abstract String colorScale();

    @JsonProperty(FIELD_REVERSE_SCALE)
    public abstract boolean reverseScale();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_NetworkVisualizationConfigDTO.Builder()
                    .colorScale("YlOrRd")
                    .reverseScale(false);
        }

        @JsonProperty(FIELD_COLOR_SCALE)
        public abstract Builder colorScale(String colorScale);

        @JsonProperty(FIELD_REVERSE_SCALE)
        public abstract Builder reverseScale(boolean reverseScale);

        public abstract NetworkVisualizationConfigDTO build();
    }
}
