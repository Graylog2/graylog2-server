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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.contentpacks.model.entities.references.ValueType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

public class ValueTypeDeserializer extends StdDeserializer<ValueType> {
    public ValueTypeDeserializer() {
        super(ValueType.class);
    }

    @Override
    public ValueType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentTokenId() == JsonTokenId.ID_STRING) {
            final String str = StringUtils.upperCase(p.getText(), Locale.ROOT);
            try {
                return ValueType.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ctxt.weirdStringException(str, ValueType.class, e.getMessage());
            }
        } else {
            throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, "expected String " + Arrays.toString(ValueType.values()));
        }
    }
}
