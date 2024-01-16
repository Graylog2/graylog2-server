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
package org.graylog2.datatiering.fallback;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotNull;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.datatiering.DataTieringConfig;
import org.joda.time.Period;

import static org.graylog2.indexer.rotation.tso.IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MAX;
import static org.graylog2.indexer.rotation.tso.IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MIN;

@AutoValue
@WithBeanGetter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect
public abstract class FallbackDataTieringConfig implements DataTieringConfig {

    public static final String TYPE = "fallback";

    @JsonCreator
    public static FallbackDataTieringConfig create(@JsonProperty(FIELD_INDEX_LIFETIME_MIN) @NotNull Period indexLifetimeMin,
                                                   @JsonProperty(FIELD_INDEX_LIFETIME_MAX) @NotNull Period indexLifetimeMax) {
        return new AutoValue_FallbackDataTieringConfig(TYPE, indexLifetimeMin, indexLifetimeMax);
    }
}
