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
package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class SizeBasedRotationStrategyResponse implements DeflectorConfigResponse {
    @JsonProperty("max_size_per_index")
    public abstract long maxSizePerIndex();

    public static SizeBasedRotationStrategyResponse create(@JsonProperty(TYPE_FIELD) String type,
                                                           @JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_size_per_index") long maxSizePerIndex) {
        return new AutoValue_SizeBasedRotationStrategyResponse(type, maxNumberOfIndices, maxSizePerIndex);
    }

    public static SizeBasedRotationStrategyResponse create(@JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_size_per_index") long maxSizePerIndex) {
        return create(SizeBasedRotationStrategyResponse.class.getCanonicalName(), maxNumberOfIndices, maxSizePerIndex);
    }
}
