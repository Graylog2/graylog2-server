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

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class CustomModelConverter implements ModelConverter {

    @Override
    public boolean isOpenapi31() {
        return true;
    }

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        // At the moment we only wrap the context to handle OptionalInt, OptionalLong, and OptionalDouble types.
        return chain.hasNext() ? chain.next().resolve(type, new CustomConverterContext(context), chain) : null;
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
