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

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = TimeBasedRotationStrategyConfig.Builder.class)
public abstract class TimeBasedRotationStrategyConfig implements RotationStrategyConfig {
    private static final Period DEFAULT_DAYS = Period.days(1);

    @JsonProperty("rotation_period")
    public abstract Period rotationPeriod();

    @JsonProperty("max_rotation_period")
    @Nullable
    public abstract Period maxRotationPeriod();

    @JsonProperty("rotate_empty_index_set")
    public abstract boolean rotateEmptyIndexSet();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TimeBasedRotationStrategyConfig.Builder()
                    .type(TimeBasedRotationStrategyConfig.class.getCanonicalName())
                    .rotationPeriod(DEFAULT_DAYS)
                    .rotateEmptyIndexSet(false);
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("rotation_period")
        public abstract Builder rotationPeriod(Period rotationPeriod);

        @JsonProperty("max_rotation_period")
        @Nullable
        public abstract Builder maxRotationPeriod(Period maxRotationPeriod);

        @JsonProperty("rotate_empty_index_set")
        public abstract Builder rotateEmptyIndexSet(boolean rotateEmptyIndexSet);

        public abstract TimeBasedRotationStrategyConfig build();
    }
}
