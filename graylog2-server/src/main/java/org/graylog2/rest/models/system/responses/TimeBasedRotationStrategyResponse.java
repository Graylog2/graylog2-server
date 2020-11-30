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
import org.joda.time.Period;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class TimeBasedRotationStrategyResponse implements DeflectorConfigResponse {
    @JsonProperty("max_time_per_index")
    public abstract Period maxTimePerIndex();

    public static TimeBasedRotationStrategyResponse create(@JsonProperty(TYPE_FIELD) String type,
                                                           @JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_time_per_index") Period maxTimePerIndex) {
        return new AutoValue_TimeBasedRotationStrategyResponse(type, maxNumberOfIndices, maxTimePerIndex);
    }

    public static TimeBasedRotationStrategyResponse create(@JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_time_per_index") Period maxTimePerIndex) {
        return create(TimeBasedRotationStrategyResponse.class.getCanonicalName(), maxNumberOfIndices, maxTimePerIndex);
    }
}
