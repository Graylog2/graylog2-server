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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.graylog2.contentpacks.model.entities.references.ValueType;

import java.io.IOException;
import java.util.Locale;

public class ValueTypeSerializer extends StdSerializer<ValueType> {
    public ValueTypeSerializer() {
        super(ValueType.class);
    }

    @Override
    public void serialize(ValueType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.name().toLowerCase(Locale.ROOT));
    }
}
