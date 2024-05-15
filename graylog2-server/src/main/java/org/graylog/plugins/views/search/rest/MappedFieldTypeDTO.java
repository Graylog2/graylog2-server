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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.formatting.units.model.UnitId;
import org.graylog2.indexer.fieldtypes.FieldTypes;

import javax.annotation.Nullable;

@JsonAutoDetect
public record MappedFieldTypeDTO(@JsonProperty("name") String name,
                                 @JsonProperty("type") FieldTypes.Type type,
                                 @JsonProperty("unit") @Nullable UnitId unit
) {

    public static MappedFieldTypeDTO create(String name, FieldTypes.Type type) {
        return new MappedFieldTypeDTO(name, type, null);
    }
}
