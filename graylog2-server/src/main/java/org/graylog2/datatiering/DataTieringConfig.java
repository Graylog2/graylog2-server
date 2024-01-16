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
package org.graylog2.datatiering;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import org.graylog2.datatiering.fallback.FallbackDataTieringConfig;
import org.joda.time.Period;

import static org.graylog2.indexer.rotation.tso.IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MAX;
import static org.graylog2.indexer.rotation.tso.IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MIN;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.EXISTING_PROPERTY,
              property = DataTieringConfig.FIELD_TYPE,
              visible = true,
              defaultImpl = FallbackDataTieringConfig.class)
public interface DataTieringConfig {

    String FIELD_TYPE = "type";

    @NotNull
    @JsonProperty(FIELD_TYPE)
    String type();

    @NotNull
    @JsonProperty(FIELD_INDEX_LIFETIME_MIN)
    Period indexLifetimeMin();

    @NotNull
    @JsonProperty(FIELD_INDEX_LIFETIME_MAX)
    Period indexLifetimeMax();

}
