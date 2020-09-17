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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class EncryptedValueDeserializer extends StdDeserializer<EncryptedValue> {
    private final EncryptedValueService encryptedValueService;

    public EncryptedValueDeserializer(EncryptedValueService encryptedValueService) {
        super(EncryptedValue.class);
        this.encryptedValueService = encryptedValueService;
    }

    @Override
    public EncryptedValue getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        return EncryptedValue.createUnset();
    }

    @Override
    public EncryptedValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final JsonNode node = p.getCodec().readTree(p);

        // If the database type is enable, we want to read the value from the database and so we parse
        // the encrypted value and the salt.
        if (EncryptedValueMapperConfig.isDatabase(ctxt)) {
            return parseFromDatabase(p, node);
        } else {
            // If the database type is not enabled, we want to create a new value and encrypt the "set_value" content.
            // (e.g. parsing a HTTP request)
            return parseSetValue(p, node);
        }
    }

    private EncryptedValue parseFromDatabase(JsonParser p, JsonNode node) throws JsonProcessingException {
        final JsonNode value = node.path("encrypted_value");
        final JsonNode salt = node.path("salt");

        if (value.isTextual() && salt.isTextual()) {
            return EncryptedValue.builder()
                    .value(value.asText())
                    .salt(salt.asText())
                    .build();
        }

        throw new JsonMappingException(p, "Couldn't deserialize value: " + node.toString() + " (encrypted_value and salt must be a strings and cannot missing)");
    }

    private EncryptedValue parseSetValue(JsonParser p, JsonNode node) throws JsonProcessingException {
        final JsonNode setValue = node.isTextual() ? node : node.path("set_value");
        if (setValue.isTextual() && !isBlank(setValue.asText())) {
            return encryptedValueService.encrypt(setValue.asText());
        }

        throw new JsonMappingException(p, "Couldn't deserialize value: " + node.toString() + " (set_value must be a string and cannot be empty or missing)");
    }
}
