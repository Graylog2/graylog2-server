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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.graylog2.shared.utilities.StringUtils.f;

final class JacksonProperties {
    private final ObjectMapper objectMapper;
    private final Map<Class<?>, List<BeanPropertyDefinition>> properties = new ConcurrentHashMap<>();

    JacksonProperties(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<BeanPropertyDefinition> properties(Class<?> type) {
        return properties.computeIfAbsent(type, t -> objectMapper.getSerializationConfig()
                .introspect(objectMapper.constructType(t))
                .findProperties());
    }

    String jsonName(Class<?> type, String internalName) {
        return properties(type).stream()
                .filter(property -> property.getInternalName().equals(internalName))
                .map(BeanPropertyDefinition::getName)
                .findFirst()
                .orElse(internalName);
    }

    Object value(Class<?> type, Object bean, String jsonName) {
        return properties(type).stream()
                .filter(property -> property.getName().equals(jsonName))
                .findFirst()
                .map(property -> read(property.getAccessor(), bean))
                .orElseThrow(() -> new IllegalArgumentException(f("Unknown property %s on %s", jsonName, type.getCanonicalName())));
    }

    private static Object read(AnnotatedMember accessor, Object bean) {
        if (accessor == null || bean == null) {
            return null;
        }
        accessor.fixAccess(true);
        return accessor.getValue(bean);
    }
}
