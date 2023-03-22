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
package org.graylog2.inputs.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import org.graylog2.inputs.WithInputConfiguration;
import org.graylog2.inputs.encryption.EncryptedInputConfigs;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.security.encryption.EncryptedValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Custom {@link BeanDeserializer} for input configuration values with transparent {@link EncryptedValue} handling.
 */
public class InputConfigurationDeserializer extends BeanDeserializer {
    private final BeanDeserializer deserializer;
    private final InputFieldConfigProvider inputFieldConfigProvider;

    public interface InputFieldConfigProvider {
        Optional<MessageInput.Config> getConfig(String type);
    }

    public InputConfigurationDeserializer(BeanDeserializer deserializer,
                                          InputFieldConfigProvider inputFieldConfigProvider) {
        super(deserializer);
        this.deserializer = deserializer;
        this.inputFieldConfigProvider = inputFieldConfigProvider;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final var value = deserializer.deserialize(p, ctxt);

        if (value instanceof WithInputConfiguration<?> valueWithConfiguration) {
            final var encryptedFields = getEncryptedFields(valueWithConfiguration.type());

            if (encryptedFields.isEmpty()) {
                return value;
            }

            final var objectMapper = (ObjectMapper) p.getCodec();
            final var configuration = new HashMap<>(valueWithConfiguration.configuration());

            encryptedFields.forEach(field -> {
                if (configuration.containsKey(field)) {
                    final var encryptedValue = objectMapper.convertValue(configuration.get(field), EncryptedValue.class);
                    configuration.put(field, encryptedValue);
                }
            });

            return valueWithConfiguration.withConfiguration(configuration);
        }

        return value;
    }

    private Set<String> getEncryptedFields(String type) {
        return inputFieldConfigProvider.getConfig(type)
                .map(EncryptedInputConfigs::getEncryptedFields)
                .orElse(Set.of());
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        deserializer.resolve(ctxt);
    }
}
