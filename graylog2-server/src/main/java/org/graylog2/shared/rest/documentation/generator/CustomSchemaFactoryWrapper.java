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
package org.graylog2.shared.rest.documentation.generator;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.ObjectVisitor;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.WrapperFactory;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

public class CustomSchemaFactoryWrapper extends SchemaFactoryWrapper {
    // Makes nested object schemas use this wrapper as well, not only the top-level one.
    private static final WrapperFactory WRAPPER_FACTORY = new WrapperFactory() {
        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider) {
            final SchemaFactoryWrapper wrapper = new CustomSchemaFactoryWrapper();
            wrapper.setProvider(provider);
            return wrapper;
        }

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider, VisitorContext visitorContext) {
            final SchemaFactoryWrapper wrapper = getWrapper(provider);
            wrapper.setVisitorContext(visitorContext);
            return wrapper;
        }
    };

    public CustomSchemaFactoryWrapper() {
        super(WRAPPER_FACTORY);
    }

    @Override
    public JsonObjectFormatVisitor expectObjectFormat(JavaType convertedType) {
        final ObjectSchemaWithOptionalSupport s = objectSchema(convertedType);

        final var result = visitorFactory.objectFormatVisitor(provider, s);
        ((ObjectVisitor) result).setVisitorContext(visitorContext);
        return result;
    }

    // Jackson describes interfaces without properties as "any"; polymorphic ones still need their subtypes.
    @Override
    public JsonAnyFormatVisitor expectAnyFormat(JavaType convertedType) {
        if (polymorphicSchema(convertedType).isEmpty()) {
            return super.expectAnyFormat(convertedType);
        }
        objectSchema(convertedType);
        return new JsonAnyFormatVisitor.Base();
    }

    private ObjectSchemaWithOptionalSupport objectSchema(JavaType convertedType) {
        final ObjectSchemaWithOptionalSupport s = new ObjectSchemaWithOptionalSupport();
        schema = s;

        // if we don't already have a recursive visitor context, create one
        if (visitorContext == null) {
            visitorContext = new VisitorContext();
        }

        // give each object schema a reference id and keep track of the ones we've seen
        String schemaUri = visitorContext.addSeenSchemaUri(convertedType);
        if (schemaUri != null) {
            s.setId(schemaUri);
        }

        polymorphicSchema(convertedType).ifPresent(annotation -> addSubtypes(s, annotation));
        return s;
    }

    private static Optional<Schema> polymorphicSchema(JavaType type) {
        return Optional.ofNullable(type.getRawClass().getAnnotation(Schema.class))
                .filter(annotation -> annotation.oneOf().length > 0);
    }

    private void addSubtypes(ObjectSchemaWithOptionalSupport s, Schema annotation) {
        final List<JsonSchema> subtypes = new ArrayList<>();
        final Map<String, String> mapping = new LinkedHashMap<>();
        for (Class<?> subtype : annotation.oneOf()) {
            final JavaType subtypeType = provider.constructType(subtype);
            subtypes.add(subtypeSchema(subtypeType));
            typeName(annotation, subtype).ifPresent(name -> mapping.put(name, visitorContext.javaTypeToUrn(subtypeType)));
        }

        final Map<String, Object> discriminator = new LinkedHashMap<>();
        if (!annotation.discriminatorProperty().isEmpty()) {
            discriminator.put(ObjectSchemaWithOptionalSupport.DISCRIMINATOR_PROPERTY_NAME, annotation.discriminatorProperty());
            discriminator.put(ObjectSchemaWithOptionalSupport.DISCRIMINATOR_MAPPING, mapping);
        }
        s.setSubtypes(subtypes, discriminator.isEmpty() ? null : discriminator);
    }

    private JsonSchema subtypeSchema(JavaType subtype) {
        final CustomSchemaFactoryWrapper wrapper = new CustomSchemaFactoryWrapper();
        wrapper.setProvider(provider);
        wrapper.setVisitorContext(visitorContext);
        try {
            provider.findValueSerializer(subtype).acceptJsonFormatVisitor(wrapper, subtype);
        } catch (JsonMappingException e) {
            throw new UncheckedIOException(f("Unable to describe subtype <%s>", subtype), e);
        }
        return wrapper.finalSchema();
    }

    // The type name comes from the discriminator mapping, falling back to the subtype's @JsonTypeName.
    private static Optional<String> typeName(Schema annotation, Class<?> subtype) {
        for (DiscriminatorMapping mapping : annotation.discriminatorMapping()) {
            if (mapping.schema().equals(subtype)) {
                return Optional.of(mapping.value());
            }
        }
        return Optional.ofNullable(subtype.getAnnotation(JsonTypeName.class)).map(JsonTypeName::value);
    }
}
