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
package org.graylog2.indexer.rotation.strategies;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.joda.time.Period;


@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = TimeBasedSizeOptimizingStrategyConfig.Builder.class)
public abstract class TimeBasedSizeOptimizingStrategyConfig implements RotationStrategyConfig {
    public static final String INDEX_LIFETIME_MIN = "index_lifetime_min";
    public static final String INDEX_LIFETIME_MAX = "index_lifetime_max";

    public static final Period DEFAULT_LIFETIME_MIN = Period.days(30);
    public static final Period DEFAULT_LIFETIME_MAX = Period.days(40);
    @JsonProperty(INDEX_LIFETIME_MIN)
    public abstract Period indexLifetimeMin();

    @JsonProperty(INDEX_LIFETIME_MAX)
    public abstract Period indexLifetimeMax();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TimeBasedSizeOptimizingStrategyConfig.Builder()
                    .type(TimeBasedSizeOptimizingStrategyConfig.class.getCanonicalName())
                    .indexLifetimeMin(DEFAULT_LIFETIME_MIN)
                    .indexLifetimeMax(DEFAULT_LIFETIME_MAX);
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty(INDEX_LIFETIME_MIN)
        public abstract Builder indexLifetimeMin(Period softLimit);

        @JsonProperty(INDEX_LIFETIME_MAX)
        public abstract Builder indexLifetimeMax(Period hardLimit);

        public abstract TimeBasedSizeOptimizingStrategyConfig build();
    }
}
