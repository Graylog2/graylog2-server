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

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = SmartRotationStrategyConfig.Builder.class)
public abstract class SmartRotationStrategyConfig implements RotationStrategyConfig {
    private static final int DEFAULT_MIN_DAYS = 30;
    private static final int DEFAULT_MAX_DAYS = 40;

    // TODO this is just a draft. Maybe use Period instead of multiples of days?
    @JsonProperty("max_rotation_days")
    public abstract int maxRotationDays();

    @JsonProperty("min_rotation_days")
    public abstract int minRotationDays();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_SmartRotationStrategyConfig.Builder()
                    .type(SmartRotationStrategyConfig.class.getCanonicalName())
                    .minRotationDays(DEFAULT_MIN_DAYS)
                    .maxRotationDays(DEFAULT_MAX_DAYS);
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);


        @JsonProperty("max_rotation_days")
        public abstract Builder maxRotationDays(int days);

        @JsonProperty("min_rotation_days")
        public abstract Builder minRotationDays(int days);

        public abstract SmartRotationStrategyConfig build();
    }
}
