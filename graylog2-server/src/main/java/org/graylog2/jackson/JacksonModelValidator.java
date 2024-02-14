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
package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JacksonModelValidator {
    private static final Logger LOG = LoggerFactory.getLogger(JacksonModelValidator.class);

    public static void check(String collectionName, ObjectMapper objectMapper, Class<?> clazz) {
        check(collectionName, objectMapper, clazz, new HashSet<>());
    }

    public static void check(String collectionName, ObjectMapper objectMapper, Class<?> clazz, Set<Class<?>> seen) {
        LOG.info("CHECK [{}] {}", collectionName, clazz.getCanonicalName());

        if (seen.contains(clazz)) {
            LOG.info("    SKIPPED [{}] {}", collectionName, clazz.getCanonicalName());
            return;
        }

        final var config = objectMapper.getSerializationConfig();
        final var ai = config.getAnnotationIntrospector();
        final var beanDesc = config.introspect(objectMapper.constructType(clazz));

        try {
            objectMapper.getSerializerProviderInstance().findTypedValueSerializer(clazz, true, null);
        } catch (JsonMappingException e) {
            throw new UncheckedIOException(e);
        }
        seen.add(clazz);

        if (beanDesc.getClassInfo().hasAnnotation(JsonSubTypes.class)) {
            final List<NamedType> subtypes = ai.findSubtypes(beanDesc.getClassInfo());
            if (subtypes != null) {
                for (NamedType subtype : subtypes) {
                    LOG.info("  CHECK SUBTYPE [{}] {}", collectionName, subtype.getType().getCanonicalName());
                    check(collectionName, objectMapper, subtype.getType(), seen);
                }
            }
        }
    }

    public static BeanSerializerModifier getBeanSerializerModifier() {
        return new ModelValidationBeanSerializerModifier();
    }

    private static class ModelValidationBeanSerializerModifier extends BeanSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
            final var annotatedClass = beanDesc.getClassInfo();

            if (annotatedClass.hasAnnotation(JsonTypeInfo.class)) {
                final var fieldNames = beanDesc.findProperties().stream()
                        .map(BeanPropertyDefinition::getName)
                        .collect(Collectors.toSet());

                final var jsonTypeInfo = annotatedClass.getAnnotation(JsonTypeInfo.class);
                switch (jsonTypeInfo.include()) {
                    case PROPERTY -> {
                        if (fieldNames.contains(jsonTypeInfo.property())) {
                            throw new RuntimeException("JsonTypeInfo#property value conflicts with existing property: " + jsonTypeInfo.property() + " (class " + annotatedClass.getName() + ")");
                        }
                    }
                    case EXISTING_PROPERTY -> {
                        if (!fieldNames.contains(jsonTypeInfo.property())) {
                            throw new RuntimeException("JsonTypeInfo#property value doesn't exist as property: " + jsonTypeInfo.property() + " (class " + annotatedClass.getName() + ")");
                        }
                    }
                    default -> {
                        // Nothing
                    }
                }
            }
            return beanProperties;
        }
    }
}
