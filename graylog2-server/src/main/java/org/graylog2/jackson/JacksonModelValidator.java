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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Validates model or DTO objects for correct annotation usage. It mainly checks if the configured annotations
 * for subtypes use the correct settings.
 */
public class JacksonModelValidator {
    private static final Logger LOG = LoggerFactory.getLogger(JacksonModelValidator.class);

    /**
     * Validates a class by inspecting its Jackson specific annotations. Can be used to validate classes on
     * application startup. The bean serializer modifier only runs when the first object serialization happens.
     *
     * @param collectionName the collection name that the given class is stored in
     * @param objectMapper   the Jackson object mapper
     * @param clazz          the class that should be validated
     */
    public static void check(String collectionName, ObjectMapper objectMapper, Class<?> clazz) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("CHECK [{}] {}", collectionName, clazz.getCanonicalName());
        }

        final var config = objectMapper.getSerializationConfig();
        final var ai = config.getAnnotationIntrospector();
        final var beanDesc = config.introspect(objectMapper.constructType(clazz));

        try {
            objectMapper.getSerializerProviderInstance().findTypedValueSerializer(clazz, true, null);
        } catch (JsonMappingException e) {
            throw new UncheckedIOException(e);
        }

        // AnnotationIntrospector#findSubtypes finds all subtypes going back up the parent class chain. That can lead
        // to recursion, so we only try to find subtypes for classes that are annotated with JsonSubTypes.
        if (beanDesc.getBeanClass().isAnnotationPresent(JsonSubTypes.class)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ITERATE SUBTYPES [{}] {}", collectionName, clazz.getCanonicalName());
            }
            final List<NamedType> subtypes = ai.findSubtypes(beanDesc.getClassInfo());
            if (subtypes != null) {
                for (NamedType subtype : subtypes) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("CHECK SUBTYPE [{}] {} -> {}", collectionName, clazz.getCanonicalName(), subtype.getType().getCanonicalName());
                    }
                    check(collectionName, objectMapper, subtype.getType());
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
                            throw new RuntimeException(f("JsonTypeInfo#property value conflicts with existing property: %s (class %s)", jsonTypeInfo.property(), annotatedClass.getName()));
                        }
                        if (jsonTypeInfo.use() == JsonTypeInfo.Id.NAME
                                && annotatedClass.hasAnnotation(JsonSubTypes.class)
                                && !annotatedClass.hasAnnotation(JsonTypeIdResolver.class)) {
                            // When using abstract classes that don't have a @JsonTypeName annotation as the value
                            // for @JsonSubTypes.Type annotations, Jackson cannot look up the "name" value for the
                            // subtype and will use the class name as a fallback. (e.g., {"type": "AutoValue_ClassName"})
                            // This is not an issue when a custom @JsonTypeIdResolver is present on the superclass.
                            final var invalidClasses = Arrays.stream(annotatedClass.getAnnotation(JsonSubTypes.class).value())
                                    .map(JsonSubTypes.Type::value)
                                    .map(config::constructType)
                                    .filter(JavaType::isAbstract)
                                    .map(JavaType::getRawClass)
                                    .filter(clazz -> !clazz.isAnnotationPresent(JsonTypeName.class))
                                    .map(Class::getCanonicalName)
                                    .toList();

                            if (!invalidClasses.isEmpty()) {
                                throw new RuntimeException(f("@JsonSubTypes.Type values that are abstract classes (e.g., auto-value) must have a @JsonTypeName annotation or a custom @JsonTypeIdResolver. Affected classes: %s", invalidClasses));
                            }
                        }
                    }
                    case EXISTING_PROPERTY -> {
                        if (!fieldNames.contains(jsonTypeInfo.property())) {
                            throw new RuntimeException(f("JsonTypeInfo#property value doesn't exist as property: %s (class %s)", jsonTypeInfo.property(), annotatedClass.getName()));
                        }
                    }
                    default -> {
                        // Nothing to do
                    }
                }
            }
            return beanProperties;
        }
    }
}
