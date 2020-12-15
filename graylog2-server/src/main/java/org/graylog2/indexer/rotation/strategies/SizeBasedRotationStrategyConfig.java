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
import com.github.joschi.jadconfig.util.Size;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import javax.validation.constraints.Min;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class SizeBasedRotationStrategyConfig implements RotationStrategyConfig {
    private static final long DEFAULT_MAX_SIZE = Size.gigabytes(1L).toBytes();

    @JsonProperty("max_size")
    public abstract long maxSize();

    @JsonCreator
    public static SizeBasedRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                         @JsonProperty("max_size") @Min(1) long maxSize) {
        return new AutoValue_SizeBasedRotationStrategyConfig(type, maxSize);
    }

    @JsonCreator
    public static SizeBasedRotationStrategyConfig create(@JsonProperty("max_size") @Min(1) long maxSize) {
        return create(SizeBasedRotationStrategyConfig.class.getCanonicalName(), maxSize);
    }

    public static SizeBasedRotationStrategyConfig createDefault() {
        return create(DEFAULT_MAX_SIZE);
    }
}
