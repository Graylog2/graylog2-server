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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import jakarta.validation.Valid;

@AutoValue
@JsonClassDescription("World map: the values of a group-by on a field with geo coordinates, as circles on a map.")
@JsonTypeName(WorldMapVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = WorldMapVisualizationConfigDTO.Builder.class)
public abstract class WorldMapVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "map";

    @JsonPropertyDescription("The part of the map that is shown.")
    @JsonProperty
    public abstract Viewport viewport();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("viewport")
        public abstract Builder viewport(@Valid Viewport viewport);

        public abstract WorldMapVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_WorldMapVisualizationConfigDTO.Builder()
                    .viewport(Viewport.Builder.builder().centerX(0).centerY(0).zoom(1).build());
        }
    }
}
