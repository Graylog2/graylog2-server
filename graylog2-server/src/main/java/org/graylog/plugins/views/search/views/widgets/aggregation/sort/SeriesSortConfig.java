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
package org.graylog.plugins.views.search.views.widgets.aggregation.sort;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonTypeName(SeriesSortConfig.Type)
public abstract class SeriesSortConfig implements SortConfigDTO {
    public static final String Type = "series";

    @Override
    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @Override
    @JsonProperty(FIELD_FIELD)
    public abstract String field();

    @Override
    @JsonProperty(FIELD_DIRECTION)
    public abstract Direction direction();

    @JsonCreator
    public static SeriesSortConfig create(@JsonProperty(FIELD_FIELD) String field,
                                          @JsonProperty(FIELD_DIRECTION) Direction direction) {
        return new AutoValue_SeriesSortConfig(SeriesSortConfig.Type, field, direction);
    }
}
