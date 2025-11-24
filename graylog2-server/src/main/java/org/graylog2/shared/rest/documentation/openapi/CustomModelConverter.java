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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Custom implementation of {@link ModelResolver} for OpenAPI schema generation.
 * <p>
 * This converter provides custom handling for resolving subtypes and discriminator mappings,
 * especially for types annotated with {@link com.fasterxml.jackson.annotation.JsonSubTypes}.
 * It also includes a workaround for handling Java primitive Optional types ({@link OptionalInt},
 * {@link OptionalLong}, {@link OptionalDouble}) during schema generation.
 */
public class CustomModelConverter extends ModelResolver {

    @Inject
    public CustomModelConverter(ObjectMapper applicationObjectMapper) {
        super(applicationObjectMapper.copy());
        setOpenapi31(true);
        _mapper.registerModule(
                new SimpleModule("registeredSubtypes", Version.unknownVersion()) {
                    @Override
                    public void setupModule(SetupContext context) {
                        context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                            @Override
                            public List<NamedType> findSubtypes(Annotated a) {
                                return CustomModelConverter.this.findRegisteredSubtypes(a);
                            }
                        });
                    }
                });
    }

    @Override
    public Schema<?> resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {
        // TODO: this would be the place to implement dynamic subtype resolving based on the subtypes registered
        //   with the object mapper. ideally we want oneOf and allOf
        return super.resolve(annotatedType, new CustomConverterContext(context), next);
    }

    /**
     * Resolves the OpenAPI {@link Discriminator} for the given Java type.
     * <p>
     * If the type is annotated with {@link com.fasterxml.jackson.annotation.JsonSubTypes} and the
     * discriminator mapping is missing or empty, this method populates the mapping with subtype names
     * and their corresponding schema references.
     * <p>
     * Without this custom implementation, the discriminator mapping would remain empty unless an explicit @{@link io.swagger.v3.oas.annotations.media.Schema}
     * annotation was provided.
     */
    @Override
    protected Discriminator resolveDiscriminator(JavaType type, ModelConverterContext context) {
        final var wrappedContext = new CustomConverterContext(context);
        final var discriminator = super.resolveDiscriminator(type, wrappedContext);
        if (discriminator != null && discriminator.getPropertyName() != null &&
                (discriminator.getMapping() == null || discriminator.getMapping().isEmpty())) {
            final var namedClass = _mapper.getDeserializationConfig().introspectClassAnnotations(type).getClassInfo();
            findRegisteredSubtypes(namedClass).stream()
                    .filter(subtype -> !subtype.getType().equals(type.getRawClass()))
                    .forEach(subtype ->
                            discriminator.mapping(StringUtils.defaultString(subtype.getName()), RefUtils.constructRef(
                                    wrappedContext.resolve(new AnnotatedType().type(subtype.getType())).getName())));
        }
        return discriminator;
    }

    private List<NamedType> findRegisteredSubtypes(Annotated baseType) {
        final var config = _mapper.getDeserializationConfig().with(new JacksonAnnotationIntrospector());
        return (List<NamedType>) _mapper.getSubtypeResolver().collectAndResolveSubtypesByClass(config, (AnnotatedClass) baseType);
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
