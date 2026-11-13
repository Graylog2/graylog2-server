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
package org.graylog2.indexer.indexset.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * The maximum number of fields per index of an index set. Null means the search backend default applies.
 */
public interface FieldLimitField {

    String FIELD_FIELD_LIMIT = "field_limit";
    int DEFAULT_FIELD_LIMIT = 1000;
    int MAX_FIELD_LIMIT = 2000;

    @Nullable
    @Min(DEFAULT_FIELD_LIMIT)
    @Max(MAX_FIELD_LIMIT)
    @JsonProperty(FIELD_FIELD_LIMIT)
    Integer fieldLimit();

    interface FieldLimitFieldBuilder<T> {

        @JsonProperty(FIELD_FIELD_LIMIT)
        T fieldLimit(@Nullable Integer fieldLimit);
    }
}
