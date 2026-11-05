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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.ObjectVisitor;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.WrapperFactory;
import com.fasterxml.jackson.module.jsonSchema.jakarta.types.ObjectSchema;

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
        ObjectSchema s = new ObjectSchemaWithOptionalSupport();
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

        final var result = visitorFactory.objectFormatVisitor(provider, s);
        ((ObjectVisitor) result).setVisitorContext(visitorContext);
        return result;
    }
}
