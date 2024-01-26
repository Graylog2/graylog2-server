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
package org.graylog2.indexer.rotation.tso;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotNull;
import org.joda.time.Period;


@AutoValue
public abstract class IndexLifetimeConfig {
    public static final String FIELD_INDEX_LIFETIME_MIN = "index_lifetime_min";
    public static final String FIELD_INDEX_LIFETIME_MAX = "index_lifetime_max";
    public static final Period DEFAULT_LIFETIME_MIN = Period.days(30);
    public static final Period DEFAULT_LIFETIME_MAX = Period.days(40);

    public static Builder builder() {
        return new AutoValue_IndexLifetimeConfig.Builder();
    }

    public abstract Builder toBuilder();

    @NotNull
    @JsonProperty(FIELD_INDEX_LIFETIME_MIN)
    public abstract Period indexLifetimeMin();

    @NotNull
    @JsonProperty(FIELD_INDEX_LIFETIME_MAX)
    public abstract Period indexLifetimeMax();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static IndexLifetimeConfig.Builder create() {
            return new AutoValue_IndexLifetimeConfig.Builder();
        }

        @JsonProperty(FIELD_INDEX_LIFETIME_MIN)
        public abstract Builder indexLifetimeMin(Period indexLifetimeMin);

        @JsonProperty(FIELD_INDEX_LIFETIME_MAX)
        public abstract Builder indexLifetimeMax(Period indexLifetimeMax);

        public abstract IndexLifetimeConfig build();
    }
}
