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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;

import java.util.Set;
import java.util.function.Function;

public class MessageFieldTypeMapper implements Function<RequestedField, ResponseSchemaEntry> {

    private final Set<MappedFieldTypeDTO> knownFields;

    public MessageFieldTypeMapper(Set<MappedFieldTypeDTO> knownFields) {
        this.knownFields = knownFields;
    }

    @Override
    public ResponseSchemaEntry apply(RequestedField field) {
        ResponseEntryDataType type = knownFields.stream()
                .filter(f -> f.name().equals(field.name()))
                .findFirst()
                .map(MappedFieldTypeDTO::type)
                .map(ResponseEntryDataType::fromFieldType)
                .orElse(ResponseEntryDataType.UNKNOWN);
        return ResponseSchemaEntry.field(field.toString(), type);
    }
}
