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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.stream.Stream;

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

        if (EncryptedValueMapperConfig.isDatabase(ctxt)) {
            // If the database type is enable, we want to read the value from the database and so we parse
            // the encrypted value and the salt.
            return parseFromDatabase(p, node);
        } else {
            validateValue(p, node);
            // If the database type is not enabled, we want to create a new value and encrypt the "set_value" content.
            // (e.g. parsing a HTTP request)
            if (!node.path("keep_value").isMissingNode()) {
                return parseKeepValue(p, node);
            } else if (!node.path("delete_value").isMissingNode()) {
                return parseDeleteValue(p, node);
            } else {
                return parseSetValue(p, node);
            }
        }
    }

    private EncryptedValue parseFromDatabase(JsonParser p, JsonNode node) throws JsonProcessingException {
        final JsonNode value = node.path("encrypted_value");
        final JsonNode salt = node.path("salt");

        if (value.isTextual() && salt.isTextual()) {
            return EncryptedValue.builder()
                    .value(value.asText())
                    .salt(salt.asText())
                    .isKeepValue(false)
                    .isDeleteValue(false)
                    .build();
        }

        throw new JsonMappingException(p, "Couldn't deserialize value: " + node.toString() + " (encrypted_value and salt must be a strings and cannot missing)");
    }

    private void validateValue(JsonParser p, JsonNode node) throws JsonMappingException {
        if (node.isTextual()) {
            // The node is a new password, no need to validate
            return;
        }
        final long count = Stream.of("keep_value", "delete_value", "set_value")
                .map(node::path)
                .filter(jsonNode -> !jsonNode.isMissingNode())
                .count();

        // Only one of the keys can be used at a time to make sure we don't have to need a priority for them
        if (count > 1) {
            throw new JsonMappingException(p, "Couldn't deserialize value: " + node.toString() + " (keep_value, delete_value and set_value are mutually exclusive)");
        }
    }

    private EncryptedValue parseKeepValue(JsonParser p, JsonNode node) throws JsonProcessingException {
        final JsonNode keepValue = node.path("keep_value");
        if (keepValue.isBoolean() && keepValue.booleanValue()) {
            return EncryptedValue.createWithKeepValue();
        }

        throw new JsonMappingException(p, "Couldn't deserialize value: " + node.toString() + " (keep_value must be a boolean and true)");
    }

    private EncryptedValue parseDeleteValue(JsonParser p, JsonNode node) throws JsonProcessingException {
        final JsonNode deleteValue = node.path("delete_value");
        if (deleteValue.isBoolean() && deleteValue.booleanValue()) {
            return EncryptedValue.createWithDeleteValue();
        }

        throw new JsonMappingException(p, "Couldn't deserialize value: " + node.toString() + " (delete_value must be a boolean and true)");
    }

    private EncryptedValue parseSetValue(JsonParser p, JsonNode node) throws JsonProcessingException {
        final JsonNode setValue = node.isTextual() ? node : node.path("set_value");
        if (setValue.isTextual() && !isBlank(setValue.asText())) {
            return encryptedValueService.encrypt(setValue.asText());
        }

        throw new JsonMappingException(p, "Couldn't deserialize value: " + node.toString() + " (set_value must be a string and cannot be empty or missing)");
    }
}
