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
package org.graylog2.security.encryption;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class EncryptedValueSerializer extends StdSerializer<EncryptedValue> {
    public EncryptedValueSerializer() {
        super(EncryptedValue.class);
    }

    @Override
    public void serialize(EncryptedValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        if (EncryptedValueMapperConfig.isDatabase(provider)) {
            // If we want to store this field into the database, we serialize the actual content
            gen.writeStringField("encrypted_value", value.value());
            gen.writeStringField("salt", value.salt());
        } else {
            // In all other contexts, we just serialize the "is_set" field (e.g. HTTP response)
            gen.writeBooleanField("is_set", value.isSet());
        }

        gen.writeEndObject();
    }
}
