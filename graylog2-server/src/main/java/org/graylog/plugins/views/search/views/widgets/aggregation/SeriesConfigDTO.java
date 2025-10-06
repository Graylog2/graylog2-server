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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@JsonDeserialize(builder = SeriesConfigDTO.Builder.class)
public abstract class SeriesConfigDTO {
    static final String FIELD_NAME = "name";
    static final String FIELD_THRESHOLDS = "thresholds";

    public static SeriesConfigDTO empty() {
        return Builder.builder().build();
    }

    @JsonProperty(FIELD_NAME)
    @Nullable
    public abstract String name();

    @JsonProperty(FIELD_THRESHOLDS)
    public abstract List<ThresholdConfigDTO> thresholds();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_NAME)
        public abstract Builder name(@Nullable String name);

        @JsonProperty(FIELD_THRESHOLDS)
        public Builder safeThresholds(List<ThresholdConfigDTO> thresholds) {
            return thresholds(firstNonNull(thresholds, List.of()));
        }

        abstract Builder thresholds(List<ThresholdConfigDTO> thresholds);

        public abstract SeriesConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_SeriesConfigDTO.Builder()
                    .thresholds(List.of());
        }
    }
}
