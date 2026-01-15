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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
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
import java.util.Set;

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
        super(applicationObjectMapper.copy(), fqnTypeNameResolver());
        setOpenapi31(true);
        // Workaround to hook into jackson's subtype resolution mechanism to find all registered subtypes into a
        // custom annotation processor that we register with the object mapper. Swagger resolves jackson subtypes
        // only via the mapper's annotation introspectors, and does not consider the registered subtypes. We add
        // a custom introspector that adds the registered subtypes to the ones found by the other introspectors via
        // annotations.
        _mapper.registerModule(
                new SimpleModule("registeredSubtypes", Version.unknownVersion()) {
                    @Override
                    public void setupModule(SetupContext context) {
                        context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                            @Override
                            public List<NamedType> findSubtypes(Annotated a) {
                                // Only return registered subtypes if this class declares @JsonTypeInfo
                                // (i.e., it's the actual owner of the polymorphic hierarchy)
                                if (a instanceof AnnotatedClass ac && ac.hasAnnotation(JsonTypeInfo.class)) {
                                    return CustomModelConverter.this.findRegisteredSubtypes(a);
                                }
                                return super.findSubtypes(a);
                            }
                        });
                    }
                });
    }


    @Override
    public Schema<?> resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {

        // Handle Guava immutable collections that need generic type preservation.
        // Simple scalar types are handled via PrimitiveType.customClasses() in OpenAPIContextFactory.
        if (annotatedType != null && annotatedType.getType() != null) {
            final JavaType javaType = _mapper.constructType(annotatedType.getType());
            final Class<?> rawClass = javaType.getRawClass();

            final var typeFactory = _mapper.getTypeFactory();
            if (ImmutableMap.class.isAssignableFrom(rawClass)) {
                final JavaType valueType = javaType.getContentType();
                if (valueType != null) {
                    // OpenAPI maps must have string keys, so we ignore the key type of the ImmutableMap and reconstruct
                    // an AnnotatedType for a Map<String, V> and forward to superclass
                    final JavaType mapType = typeFactory.constructMapType(Map.class,
                            typeFactory.constructType(String.class), valueType);
                    return super.resolve(replacementType(annotatedType, mapType), context, next);
                }
            } else if (ImmutableList.class.isAssignableFrom(rawClass)) {
                // ImmutableLists are just lists
                return super.resolve(
                        replacementType(annotatedType, typeFactory.constructCollectionLikeType(List.class, javaType.getContentType())),
                        context,
                        next);
            } else if (ImmutableSet.class.isAssignableFrom(rawClass)) {
                // ImmutableSets are just sets
                return super.resolve(
                        replacementType(annotatedType, typeFactory.constructCollectionLikeType(Set.class, javaType.getContentType())),
                        context,
                        next);
            } else if (Multimap.class.isAssignableFrom(rawClass)) {
                // Multimap<K, V> serializes as Map<K, Collection<V>> via GuavaModule
                final JavaType valueType = javaType.getBindings().getBoundType(1);
                if (valueType != null) {
                    final JavaType listType = typeFactory.constructCollectionLikeType(List.class, valueType);
                    final JavaType mapType = typeFactory.constructMapType(Map.class,
                            typeFactory.constructType(String.class), listType);
                    return super.resolve(replacementType(annotatedType, mapType), context, next);
                }
            } else if (Range.class.isAssignableFrom(rawClass)) {
                // Range is serialized by RangeJsonSerializer as {"start": N, "length": M}
                return new ObjectSchema()
                        .addProperty("start", new IntegerSchema())
                        .addProperty("length", new IntegerSchema());
            }
        }

        return super.resolve(annotatedType, new CustomConverterContext(context), next);
    }

    // helper to clone the given AnnotatedType with a replacement "inner" type
    private AnnotatedType replacementType(AnnotatedType previousType, Type replacement) {
        return new AnnotatedType()
                .type(replacement)
                .jsonViewAnnotation(previousType.getJsonViewAnnotation())
                .ctxAnnotations(previousType.getCtxAnnotations())
                .resolveAsRef(previousType.isResolveAsRef())
                .schemaProperty(previousType.isSchemaProperty());
    }

    /**
     * Resolves the OpenAPI {@link Discriminator} for the given Java type.
     * <p>
     * If the discriminator mapping is missing or empty, this method populates the mapping with subtype names and their
     * corresponding schema references from the Jackson annotations and the subtypes registered with the Object mapper.
     * <p>
     * Without this custom implementation, the discriminator mapping would remain empty unless an explicit
     * {@link io.swagger.v3.oas.annotations.media.Schema} annotation was provided.
     */
    @Override
    protected Discriminator resolveDiscriminator(JavaType type, ModelConverterContext context) {
        final var discriminator = super.resolveDiscriminator(type, new CustomConverterContext(context));
        if (discriminator != null && discriminator.getPropertyName() != null &&
                (discriminator.getMapping() == null || discriminator.getMapping().isEmpty())) {
            final var parentClassInfo = _mapper.getSerializationConfig().introspectClassAnnotations(type).getClassInfo();
            final var subtypes = findRegisteredSubtypes(parentClassInfo);

            for (NamedType subtype : subtypes) {
                final var subClass = subtype.getType();
                if (subClass == type.getRawClass()) {
                    continue; // skip base type
                }

                // This only works because we are overriding decorateModelName below to not include the JsonView suffix
                // Otherwise we would have to determine the currently active view, which is not trivial.
                final var schemaName = _typeName(_mapper.constructType(subClass));

                discriminator.mapping(StringUtils.defaultString(subtype.getName()), RefUtils.constructRef(schemaName));
            }
        }
        return discriminator;
    }

    @Override
    protected String decorateModelName(AnnotatedType type, String originalName) {
        // Don't add the JsonView suffix to the model name. This will break if we introduce paths with different views
        // for the same models.
        return originalName;
    }

    // uses jackson's subtype resolver to find all registered subtypes as well as the subtypes explicitly specified
    // e.g. in a @JsonSubtypes annotation for a given base type
    private List<NamedType> findRegisteredSubtypes(Annotated baseType) {
        final var config = _mapper.getSerializationConfig().with(new JacksonAnnotationIntrospector());
        return (List<NamedType>) _mapper.getSubtypeResolver().collectAndResolveSubtypesByClass(config, (AnnotatedClass) baseType);
    }

    // A TypeNameResolver that uses fully qualified names for type names
    private static TypeNameResolver fqnTypeNameResolver() {
        final var resolver = new TypeNameResolver() {};
        resolver.setUseFqn(true);
        return resolver;
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
