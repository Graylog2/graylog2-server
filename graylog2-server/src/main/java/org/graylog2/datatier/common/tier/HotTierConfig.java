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
package org.graylog2.datatier.common.tier;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.Period;

import javax.validation.constraints.NotNull;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = HotTierConfig.Builder.class)
public abstract class HotTierConfig {

    public static final String INDEX_LIFETIME_MIN = "index_lifetime_min";
    public static final String INDEX_LIFETIME_MAX = "index_lifetime_max";

    public static Builder builder() {
        return new AutoValue_HotTierConfig.Builder();
    }

    @NotNull
    @JsonProperty(INDEX_LIFETIME_MIN)
    public abstract Period indexLifetimeMin();

    @NotNull
    @JsonProperty(INDEX_LIFETIME_MAX)
    public abstract Period indexLifetimeMax();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static HotTierConfig.Builder create() {
            return new AutoValue_HotTierConfig.Builder();
        }

        @JsonProperty(INDEX_LIFETIME_MIN)
        public abstract Builder indexLifetimeMin(Period indexLifetimeMin);

        @JsonProperty(INDEX_LIFETIME_MAX)
        public abstract Builder indexLifetimeMax(Period indexLifetimeMax);

        public abstract HotTierConfig build();
    }
}
