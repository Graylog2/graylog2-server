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
package org.graylog2.rest.models.system.indices;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.Period;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class RotationStrategies {
    @JsonProperty
    public int total() {
        return strategies().size();
    }

    @JsonProperty
    public abstract List<RotationStrategyDescription> strategies();

    @JsonProperty
    public abstract RotationStrategies.Context context();

    @JsonCreator
    public static RotationStrategies create(@JsonProperty("strategies") List<RotationStrategyDescription> strategies,
                                            @JsonProperty("context") RotationStrategies.Context context) {
        return new AutoValue_RotationStrategies(strategies, context);
    }

    @AutoValue
    public static abstract class Context {
        @Nullable
        @JsonProperty("time_size_optimizing_retention_fixed_leeway")
        public abstract Period timeSizeOptimizingRotationFixedLeeway();

        @JsonCreator
        public static RotationStrategies.Context create(@Nullable @JsonProperty("time_size_optimizing_retention_fixed_leeway") Period timeSizeOptimizingRotationFixedLeeway) {
            return new AutoValue_RotationStrategies_Context(timeSizeOptimizingRotationFixedLeeway);
        }
    }
}
