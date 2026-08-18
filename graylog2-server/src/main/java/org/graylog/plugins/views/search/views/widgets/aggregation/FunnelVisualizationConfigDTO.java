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
@JsonTypeName(FunnelVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = FunnelVisualizationConfigDTO.Builder.class)
public abstract class FunnelVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "funnel";

    @JsonProperty("start_color")
    public abstract String startColor();

    @JsonProperty("end_color")
    public abstract String endColor();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("start_color")
        public abstract Builder startColor(String startColor);

        @JsonProperty("end_color")
        public abstract Builder endColor(String endColor);

        public abstract FunnelVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_FunnelVisualizationConfigDTO.Builder()
                    .startColor("#1F77B4")
                    .endColor("#AEC7E8");
        }
    }
}
