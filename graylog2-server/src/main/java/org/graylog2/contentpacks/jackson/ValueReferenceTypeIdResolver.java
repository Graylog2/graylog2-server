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
package org.graylog2.contentpacks.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.graylog2.contentpacks.model.entities.TypedEntity;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class ValueReferenceTypeIdResolver extends TypeIdResolverBase {
    private final Map<String, JavaType> subtypes;

    protected ValueReferenceTypeIdResolver(JavaType baseType, TypeFactory typeFactory, Collection<NamedType> subtypes) {
        super(baseType, typeFactory);
        this.subtypes = subtypes.stream().collect(Collectors.toMap(NamedType::getName, v -> typeFactory.constructSimpleType(v.getType(), new JavaType[0])));

    }

    @Override
    public String idFromValue(Object value) {
        if (value instanceof TypedEntity) {
            final TypedEntity typedEntity = (TypedEntity) value;
            return typedEntity.typeString();
        } else {
            return null;
        }
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return null;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        return subtypes.get(id);
    }
}
