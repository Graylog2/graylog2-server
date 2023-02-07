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

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

import java.io.IOException;

/**
 * A {@link DeserializationProblemHandler} implementation that handles missing type IDs. The handler checks if
 * the base type has a {@link JsonSubTypePropertyDefaultValue} annotation to select a default type ID.
 *
 * @see JsonSubTypePropertyDefaultValue
 */
public class MissingTypeIdDeserializationProblemHandler extends DeserializationProblemHandler {
    @Override
    public JavaType handleMissingTypeId(DeserializationContext ctxt,
                                        JavaType baseType,
                                        TypeIdResolver idResolver,
                                        String failureMsg) throws IOException {
        final JsonSubTypePropertyDefaultValue annotation = baseType.getRawClass().getAnnotation(JsonSubTypePropertyDefaultValue.class);
        if (annotation == null) {
            return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
        }
        return idResolver.typeFromId(ctxt, annotation.value());
    }
}
