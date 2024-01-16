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
package org.graylog2.datatiering;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotNull;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;


@AutoValue
@WithBeanGetter
public abstract class DataTieringState {

    private final static String FIELD_TYPE = "type";
    private static final String FIELD_WARM_TIER_REQUIREMENTS = "warm_tier_requirements";

    public static Builder builder() {
        return new $AutoValue_DataTieringState.Builder();
    }

    @NotNull
    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @NotNull
    @JsonProperty(FIELD_WARM_TIER_REQUIREMENTS)
    public abstract List<String> warmTierRequirements();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(@NotNull String type);

        @JsonProperty(FIELD_WARM_TIER_REQUIREMENTS)
        public abstract Builder warmTierRequirements(@NotNull List<String> warmTierRequirements);

        public abstract DataTieringState build();
    }
}
