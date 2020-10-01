/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
