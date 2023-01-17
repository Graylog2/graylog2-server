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

import java.time.Period;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = TimeBasedSizeOptimizingStrategyConfig.Builder.class)
public abstract class TimeBasedSizeOptimizingStrategyConfig implements RotationStrategyConfig {
    public static final String INDEX_LIFETIME_SOFT = "index_lifetime_soft";
    public static final String INDEX_LIFETIME_HARD = "index_lifetime_hard";

    private static final Period DEFAULT_LIFETIME_SOFT = Period.ofDays(30);
    private static final Period DEFAULT_LIFETIME_HARD = Period.ofDays(40);
    @JsonProperty(INDEX_LIFETIME_SOFT)
    public abstract Period indexLifetimeSoft();

    @JsonProperty(INDEX_LIFETIME_HARD)
    public abstract Period indexLifetimeHard();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TimeBasedSizeOptimizingStrategyConfig.Builder()
                    .type(TimeBasedSizeOptimizingStrategyConfig.class.getCanonicalName())
                    .indexLifetimeSoft(DEFAULT_LIFETIME_SOFT)
                    .indexLifetimeHard(DEFAULT_LIFETIME_HARD);
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty(INDEX_LIFETIME_SOFT)
        public abstract Builder indexLifetimeSoft(Period softLimit);

        @JsonProperty(INDEX_LIFETIME_HARD)
        public abstract Builder indexLifetimeHard(Period hardLimit);

        public abstract TimeBasedSizeOptimizingStrategyConfig build();
    }
}
