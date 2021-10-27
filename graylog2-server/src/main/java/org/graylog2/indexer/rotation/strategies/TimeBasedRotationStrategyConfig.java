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
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.joda.time.Period;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class TimeBasedRotationStrategyConfig implements RotationStrategyConfig {
    private static final Period DEFAULT_DAYS = Period.days(1);

    @JsonProperty("rotation_period")
    public abstract Period rotationPeriod();

    @JsonProperty("elasticsearch_max_write_index_age")
    @Nullable
    public abstract Period maxWriteIndexAge();

    @JsonCreator
    public static TimeBasedRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                         @JsonProperty("rotation_period") @NotNull Period maxTimePerIndex,
                                                         @JsonProperty("elasticsearch_max_write_index_age") Period maxWriteIndexAge) {
        return new AutoValue_TimeBasedRotationStrategyConfig(type, maxTimePerIndex, maxWriteIndexAge);
    }

    @JsonCreator
    public static TimeBasedRotationStrategyConfig create(@JsonProperty("rotation_period") @NotNull Period maxTimePerIndex,
                                                         @JsonProperty("elasticsearch_max_write_index_age") Period maxWriteIndexAge) {
        return create(TimeBasedRotationStrategyConfig.class.getCanonicalName(), maxTimePerIndex, maxWriteIndexAge);
    }

    public static TimeBasedRotationStrategyConfig createDefault(Period maxWriteIndexAge) {
        return create(DEFAULT_DAYS, maxWriteIndexAge);
    }
}
