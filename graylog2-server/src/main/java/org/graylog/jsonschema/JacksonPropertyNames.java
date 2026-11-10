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
package org.graylog.jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves schema property names the way the given {@link ObjectMapper} serializes them, including its global naming strategy.
 */
final class JacksonPropertyNames {
    private final ObjectMapper objectMapper;
    private final Map<Class<?>, List<BeanPropertyDefinition>> properties = new ConcurrentHashMap<>();

    JacksonPropertyNames(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String forField(FieldScope field) {
        final String name = field.getName();
        return properties(field.getDeclaringType().getErasedType()).stream()
                .filter(property -> (property.getField() != null && property.getField().getName().equals(name)) || property.getInternalName().equals(name))
                .map(BeanPropertyDefinition::getName)
                .findFirst()
                .orElse(null);
    }

    String forMethod(MethodScope method) {
        final String name = method.getName();
        return properties(method.getDeclaringType().getErasedType()).stream()
                .filter(property -> (property.getGetter() != null && property.getGetter().getName().equals(name)) || property.getInternalName().equals(name))
                .map(BeanPropertyDefinition::getName)
                .findFirst()
                .orElse(null);
    }

    private List<BeanPropertyDefinition> properties(Class<?> type) {
        return properties.computeIfAbsent(type, t -> objectMapper.getSerializationConfig()
                .introspect(objectMapper.constructType(t))
                .findProperties());
    }
}
