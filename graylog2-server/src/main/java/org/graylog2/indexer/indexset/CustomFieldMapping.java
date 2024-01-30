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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;

import static org.graylog2.indexer.indexset.CustomFieldMappings.AVAILABLE_TYPES;

public record CustomFieldMapping(@JsonProperty("field") String fieldName,
                                 @JsonProperty("type") String type) {
    public String toPhysicalType() {
        var typeDescription = AVAILABLE_TYPES.get(type());
        if (typeDescription == null) {
            throw new IllegalStateException("Invalid type specified: " + type());
        }

        return typeDescription.physicalType();
    }

    public FieldTypeDTO toFieldTypeDTO() {
        return FieldTypeDTO.create(
                fieldName(),
                toPhysicalType()
        );
    }
}
