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
package org.graylog2.configuration.tunables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class RuntimeTunableSecrets {
    private final JacksonProperties jacksonProperties;
    private final ObjectMapper databaseMapper;

    @Inject
    public RuntimeTunableSecrets(ObjectMapper objectMapper) {
        this.jacksonProperties = new JacksonProperties(objectMapper);
        this.databaseMapper = objectMapper.copy();
        EncryptedValueMapperConfig.enableDatabase(databaseMapper);
    }

    public Object resolve(RuntimeTunableDescriptor descriptor, Object config, @Nullable Object stored) {
        final Map<String, EncryptedValue> replacements = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> property : descriptor.schema().path("properties").properties()) {
            if (!property.getValue().path("x-graylog-secret").asBoolean(false)) {
                continue;
            }
            final EncryptedValue value = secret(descriptor.type(), config, property.getKey());
            if (value == null) {
                continue;
            }
            if (value.isKeepValue()) {
                final EncryptedValue current = stored == null ? null : secret(descriptor.type(), stored, property.getKey());
                replacements.put(property.getKey(), current == null ? EncryptedValue.createUnset() : current);
            } else if (value.isDeleteValue()) {
                replacements.put(property.getKey(), EncryptedValue.createUnset());
            }
        }
        if (replacements.isEmpty()) {
            return config;
        }

        final ObjectNode tree = databaseMapper.valueToTree(config);
        replacements.forEach((property, value) -> tree.set(property, databaseMapper.valueToTree(value)));
        try {
            return databaseMapper.treeToValue(tree, descriptor.type());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(f("Couldn't resolve secrets of runtime tunable %s", descriptor.id()), e);
        }
    }

    private EncryptedValue secret(Class<?> type, Object bean, String property) {
        return (EncryptedValue) jacksonProperties.value(type, bean, property);
    }
}
