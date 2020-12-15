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
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import java.util.Collection;

public class ValueReferenceTypeResolverBuilder extends StdTypeResolverBuilder {
    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        if (_idType != JsonTypeInfo.Id.CUSTOM || _includeAs != JsonTypeInfo.As.WRAPPER_OBJECT) {
            return super.buildTypeDeserializer(config, baseType, subtypes);
        }

        final TypeIdResolver idRes = new ValueReferenceTypeIdResolver(baseType, config.getTypeFactory(), subtypes);
        final JavaType defaultImpl;
        if (_defaultImpl == null) {
            defaultImpl = null;
        } else {
            if ((_defaultImpl == Void.class) || (_defaultImpl == NoClass.class)) {
                defaultImpl = config.getTypeFactory().constructType(_defaultImpl);
            } else {
                defaultImpl = config.getTypeFactory().constructSpecializedType(baseType, _defaultImpl);
            }
        }

        return new AsValueReferenceTypeDeserializer(baseType, idRes, _typeProperty, _typeIdVisible, defaultImpl);
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        if (_idType != JsonTypeInfo.Id.CUSTOM || _includeAs != JsonTypeInfo.As.WRAPPER_OBJECT) {
            return super.buildTypeSerializer(config, baseType, subtypes);
        }

        TypeIdResolver idRes = new ValueReferenceTypeIdResolver(baseType, config.getTypeFactory(), subtypes);
        return new AsPropertyTypeSerializer(idRes, null, this._typeProperty);
    }
}
