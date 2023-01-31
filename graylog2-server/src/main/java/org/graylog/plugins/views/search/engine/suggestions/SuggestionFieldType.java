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
package org.graylog.plugins.views.search.engine.suggestions;

import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.indexer.fieldtypes.FieldTypes;

import java.util.Arrays;
import java.util.function.Predicate;

import static org.graylog2.indexer.fieldtypes.FieldTypeMapper.STRING_TYPE;

public enum SuggestionFieldType {
    TEXTUAL(type -> (type.properties().contains(FieldTypeMapper.PROP_FULL_TEXT_SEARCH) || STRING_TYPE.equals(type))),
    NUMERICAL(field -> field.properties().contains(FieldTypeMapper.PROP_NUMERIC)),
    OTHER(props -> false);

    private final Predicate<FieldTypes.Type> matchesFieldProperty;

    SuggestionFieldType(Predicate<FieldTypes.Type> matchesFieldProperty) {
        this.matchesFieldProperty = matchesFieldProperty;
    }

    /**
     * @see FieldTypeMapper
     */
    public static SuggestionFieldType fromFieldType(FieldTypes.Type field) {
        return Arrays.stream(values())
                .filter(it -> it.matchesFieldProperty.test(field))
                .findFirst()
                .orElse(OTHER);
    }
}
