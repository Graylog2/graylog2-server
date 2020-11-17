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
public abstract class MessageCountRotationStrategyResponse implements DeflectorConfigResponse {
    @JsonProperty("max_docs_per_index")
    public abstract int maxDocsPerIndex();

    public static MessageCountRotationStrategyResponse create(@JsonProperty(TYPE_FIELD) String type,
                                                              @JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                              @JsonProperty("max_docs_per_index") int maxDocsPerIndex) {
        return new AutoValue_MessageCountRotationStrategyResponse(type, maxNumberOfIndices, maxDocsPerIndex);
    }

    public static MessageCountRotationStrategyResponse create(@JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                              @JsonProperty("max_docs_per_index") int maxDocsPerIndex) {
        return create(MessageCountRotationStrategyResponse.class.getCanonicalName(), maxNumberOfIndices, maxDocsPerIndex);
    }
}
