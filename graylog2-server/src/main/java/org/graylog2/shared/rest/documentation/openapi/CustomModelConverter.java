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
package org.graylog2.shared.rest.documentation.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.inject.Inject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class CustomModelConverter extends ModelResolver {

    @Inject
    public CustomModelConverter(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    public boolean isOpenapi31() {
        return true;
    }

    @Override
    public Schema<?> resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {
        Schema<?> baseSchema = super.resolve(annotatedType, new CustomConverterContext(context), next);
        if (true) {
            return baseSchema;
        }
        if (baseSchema == null) {
            return null;
        }

        // Determine the raw class of the annotated type
        Class<?> baseClass;
        try {
            baseClass = com.fasterxml.jackson.databind.type.TypeFactory.rawClass(annotatedType.getType());
        } catch (Exception e) {
            return baseSchema;
        }

        // If the schema already has 'oneOf' entries (from annotations), skip this logic
        if (baseSchema instanceof ComposedSchema composedSchema &&
                composedSchema.getOneOf() != null &&
                !composedSchema.getOneOf().isEmpty()) {
            return baseSchema;
        }

        // --- BEGIN: Add subtype awareness using Jackson's SubtypeResolver ---
        var serializationConfig = _mapper.getSerializationConfig();
        var parentDescription = serializationConfig.introspectClassAnnotations(baseClass);

        List<NamedType> resolvedSubtypes =
                List.copyOf(
                        _mapper.getSubtypeResolver()
                                .collectAndResolveSubtypesByClass(
                                        serializationConfig,
                                        parentDescription.getClassInfo()
                                ));

        if (resolvedSubtypes.isEmpty()) {
            return baseSchema;
        }

        List<Schema<?>> subtypeSchemas = new ArrayList<>();
        Map<String, String> discriminatorMapping = new LinkedHashMap<>();

        for (var namedSubtype : resolvedSubtypes) {
            if (namedSubtype.getType() == baseClass) {
                continue; // Skip the base class itself
            }

            // Generate a $ref schema for each registered subtype
            Schema<?> subtypeRefSchema = context.resolve(
                    new AnnotatedType()
                            .type(namedSubtype.getType())
                            .resolveAsRef(true)
            );

            if (subtypeRefSchema != null && subtypeRefSchema.get$ref() != null) {
                subtypeSchemas.add(new Schema<>().$ref(subtypeRefSchema.get$ref()));

                // Use registered name (if provided) for discriminator mapping
                if (namedSubtype.hasName()) {
                    discriminatorMapping.put(namedSubtype.getName(), subtypeRefSchema.get$ref());
                }
            }
        }

        if (subtypeSchemas.isEmpty()) {
            return baseSchema;
        }

        // Create or augment a composed schema that includes 'oneOf' subtypes
        ComposedSchema composedSchema =
                (baseSchema instanceof ComposedSchema)
                        ? (ComposedSchema) baseSchema
                        : new ComposedSchema();

        composedSchema.setOneOf(List.copyOf(subtypeSchemas));

        // Determine discriminator property name
        var jsonTypeInfoAnnotation =
                parentDescription.getClassInfo().getAnnotation(com.fasterxml.jackson.annotation.JsonTypeInfo.class);

        String discriminatorProperty =
                (jsonTypeInfoAnnotation != null &&
                        jsonTypeInfoAnnotation.use() == com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME &&
                        jsonTypeInfoAnnotation.property() != null &&
                        !jsonTypeInfoAnnotation.property().isBlank())
                        ? jsonTypeInfoAnnotation.property()
                        : "type"; // Default fallback

        // Add discriminator to the composed schema
        io.swagger.v3.oas.models.media.Discriminator discriminator =
                new io.swagger.v3.oas.models.media.Discriminator().propertyName(discriminatorProperty);

        if (!discriminatorMapping.isEmpty()) {
            discriminator.setMapping(discriminatorMapping);
        }

        composedSchema.setDiscriminator(discriminator);
        return composedSchema;
    }


    /**
     * Custom ModelConverterContext wrapper that implements a shortcut for resolving {@link OptionalInt},
     * {@link OptionalLong}, and {@link OptionalDouble} types which cause issues during schema generation.
     * <p>
     * Workaround for <a href="https://github.com/swagger-api/swagger-core/issues/4717">https://github.com/swagger-api/swagger-core/issues/4717</a>
     */
    private static class CustomConverterContext implements ModelConverterContext {
        private final ModelConverterContext delegate;

        private CustomConverterContext(ModelConverterContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public void defineModel(String name, Schema model) {
            delegate.defineModel(name, model);
        }

        @Override
        public void defineModel(String name, Schema model, AnnotatedType type, String prevName) {
            delegate.defineModel(name, model, type, prevName);
        }

        @Override
        public void defineModel(String name, Schema model, Type type, String prevName) {
            delegate.defineModel(name, model, type, prevName);
        }

        @Override
        public Schema<?> resolve(AnnotatedType type) {
            if (type == null || type.getType() == null) {
                return null;
            }

            final var javaType = Json.mapper().constructType(type.getType());
            final var rawClass = javaType.getRawClass();

            // Check if this is one of the primitive Optional types
            if (OptionalInt.class.equals(rawClass)) {
                return createIntegerSchema("int32");
            } else if (OptionalLong.class.equals(rawClass)) {
                return createIntegerSchema("int64");
            } else if (OptionalDouble.class.equals(rawClass)) {
                return createDoubleSchema();
            }

            return delegate.resolve(type);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map<String, Schema> getDefinedModels() {
            return delegate.getDefinedModels();
        }

        @Override
        public Iterator<ModelConverter> getConverters() {
            return delegate.getConverters();
        }

        private static Schema<?> createIntegerSchema(String format) {
            IntegerSchema schema = new IntegerSchema();
            schema.setFormat(format);
            return schema;
        }

        private static Schema<?> createDoubleSchema() {
            NumberSchema schema = new NumberSchema();
            schema.setFormat("double");
            return schema;
        }
    }
}
