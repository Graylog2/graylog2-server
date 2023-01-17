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
package org.graylog2.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EncryptedValuesSupport {

    private final Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories;
    private final ObjectMapper encryptingObjectMapper;
    private final ObjectMapper dbObjectMapper;

    @Inject
    public EncryptedValuesSupport(Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories, ObjectMapper objectMapper) {
        this.inputFactories = inputFactories;
        this.encryptingObjectMapper = objectMapper;
        this.dbObjectMapper = objectMapper.copy();
        EncryptedValueMapperConfig.enableDatabase(dbObjectMapper);
    }

    public Object toDbObject(EncryptedValue encryptedValue) {
        return dbObjectMapper.convertValue(encryptedValue, TypeReferences.MAP_STRING_OBJECT);
    }

    public Map<String, Object> fromUntypedConfiguration(String type, Map<String, Object> inputConfiguration) {
        return makeEncryptedValues(type, inputConfiguration, encryptingObjectMapper);
    }

    public Configuration fromUntypedConfiguration(String type, Configuration configuration) {
        return new Configuration(fromUntypedConfiguration(type, configuration.getSource()));
    }

    public Map<String, Object> fromDbObjects(String type, Map<String, Object> inputConfiguration) {
        return makeEncryptedValues(type, inputConfiguration, dbObjectMapper);
    }

    private Map<String, Object> makeEncryptedValues(String type, Map<String, Object> inputConfiguration, ObjectMapper objectMapper) {
        final Map<String, ConfigurationField> encryptedFields = getEncryptedFields(type);

        if (encryptedFields.isEmpty()) {
            return inputConfiguration;
        }

        final Map<String, Object> newConfiguration = new HashMap<>(inputConfiguration);
        encryptedFields.forEach((k, v) -> newConfiguration.computeIfPresent(k, (x, y) -> toEncryptedValue(y, objectMapper)));
        return newConfiguration;
    }

    private EncryptedValue toEncryptedValue(Object value, ObjectMapper objectMapper) {
        if (value instanceof EncryptedValue encryptedValue) {
            return encryptedValue;
        } else {
            return objectMapper.convertValue(value, EncryptedValue.class);
        }
    }

    private Map<String, ConfigurationField> getEncryptedFields(String inputType) {
        if (!inputFactories.containsKey(inputType)) {
            return Map.of();
        }

        return inputFactories.get(inputType)
                .getConfig()
                .combinedRequestedConfiguration()
                .getFields()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isEncrypted())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
