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
package org.graylog2.contentpacks.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.entities.references.Reference;
import org.graylog2.contentpacks.model.entities.references.ReferenceList;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.contentpacks.model.entities.references.ValueType;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class ReferenceConverter extends StdConverter<JsonNode, Reference> {
    // Expected field names for a ValueReference
    private static final ImmutableSet<String> EXPECTED_FIELD_NAMES = ImmutableSet.of(ValueReference.FIELD_TYPE, ValueReference.FIELD_VALUE);

    @Override
    public Reference convert(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            final ImmutableSet<String> fieldNames = ImmutableSet.copyOf(jsonNode.fieldNames());
            if (fieldNames.equals(EXPECTED_FIELD_NAMES)) {
                // TODO: Possible to use ValueTypeDeserializer to avoid duplication?
                final String valueTypeText = jsonNode.path(ValueReference.FIELD_TYPE).asText();
                final ValueType valueType = ValueType.valueOf(valueTypeText.toUpperCase(Locale.ROOT));

                final JsonNode value = jsonNode.path(ValueReference.FIELD_VALUE);
                if (valueType == ValueType.BOOLEAN && value.isBoolean()) {
                    return ValueReference.of(value.booleanValue());
                } else if (valueType == ValueType.DOUBLE && value.isDouble()) {
                    return ValueReference.of(value.doubleValue());
                } else if (valueType == ValueType.FLOAT && value.isFloat()) {
                    return ValueReference.of(value.floatValue());
                } else if (valueType == ValueType.INTEGER && value.isInt()) {
                    return ValueReference.of(value.intValue());
                } else if (valueType == ValueType.LONG && (value.isLong() || value.isInt())) {
                    //                                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                    // Jackson actually creates an int value for a small number so we check for both (long and int value) here
                    return ValueReference.of(value.longValue());
                } else if (valueType == ValueType.STRING && value.isTextual()) {
                    return ValueReference.of(value.textValue());
                } else if (valueType == ValueType.PARAMETER && value.isTextual()) {
                    return ValueReference.createParameter(value.textValue());
                } else {
                    return null;
                }
            } else {
                final ImmutableMap.Builder<String, Reference> map = ImmutableMap.builder();
                final Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> entry = fields.next();
                    map.put(entry.getKey(), convert(entry.getValue()));
                }
                return new ReferenceMap(map.build());
            }
        } else if (jsonNode.isArray()) {
            final ImmutableList.Builder<Reference> list = ImmutableList.builder();
            for (JsonNode value : jsonNode) {
                list.add(convert(value));
            }
            return new ReferenceList(list.build());
        }
        return null;
    }
}
