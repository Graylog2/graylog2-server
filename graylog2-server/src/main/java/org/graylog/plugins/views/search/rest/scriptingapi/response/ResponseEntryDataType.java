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
package org.graylog.plugins.views.search.rest.scriptingapi.response;

import com.fasterxml.jackson.annotation.JsonValue;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.indexer.fieldtypes.FieldTypes;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public enum ResponseEntryDataType {
    STRING(FieldTypeMapper.STRING_TYPE, FieldTypeMapper.STRING_FTS_TYPE),
    NUMERIC(FieldTypeMapper.LONG_TYPE, FieldTypeMapper.INT_TYPE, FieldTypeMapper.SHORT_TYPE, FieldTypeMapper.BYTE_TYPE, FieldTypeMapper.DOUBLE_TYPE, FieldTypeMapper.FLOAT_TYPE),
    DATE(FieldTypeMapper.DATE_TYPE),
    BOOLEAN(FieldTypeMapper.BOOLEAN_TYPE),
    BINARY(FieldTypeMapper.BINARY_TYPE),
    IP(FieldTypeMapper.IP_TYPE),
    GEO(FieldTypeMapper.GEO_POINT_TYPE),
    STREAM(FieldTypeMapper.STREAMS_TYPE),
    INPUT(FieldTypeMapper.INPUT_TYPE),
    NODE(FieldTypeMapper.STREAMS_TYPE),
    UNKNOWN();

    private final Set<FieldTypes.Type> correspondingFieldTypes;

    ResponseEntryDataType(FieldTypes.Type... correspondingFieldTypes) {
        this.correspondingFieldTypes = Set.of(correspondingFieldTypes);
    }

    public static ResponseEntryDataType fromFieldType(final FieldTypes.Type type) {
        if (type == null) {
            return UNKNOWN;
        }
        return Arrays.stream(ResponseEntryDataType.values())
                .filter(dataType -> dataType.correspondingFieldTypes.stream().anyMatch(corresponding -> isMatch(corresponding, type)))
                .findFirst()
                .orElse(UNKNOWN);
    }

    private static boolean isMatch(FieldTypes.Type corresponding, FieldTypes.Type type) {
        // FieldTypes.Type is not just type, it includes also properties. Properties may differ in the received and corresponding
        // type, which leads to wrong detection of a match. So we have to use the type itself, which is reliable for now.
        // TODO: can we solve that in a more elegant way?
        return corresponding.type().equals(type.type());
    }

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
