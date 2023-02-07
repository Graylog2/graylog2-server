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

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoValueSubtypeResolver extends StdSubtypeResolver {
    @Override
    protected void _collectAndResolve(AnnotatedClass annotatedType, NamedType namedType, MapperConfig<?> config, AnnotationIntrospector ai, HashMap<NamedType, NamedType> collectedSubtypes) {
        super._collectAndResolve(annotatedType, resolveAutoValue(namedType), config, ai, collectedSubtypes);
    }

    @Override
    protected void _collectAndResolveByTypeId(AnnotatedClass annotatedType, NamedType namedType, MapperConfig<?> config, Set<Class<?>> typesHandled, Map<String, NamedType> byName) {
        super._collectAndResolveByTypeId(annotatedType, resolveAutoValue(namedType), config, typesHandled, byName);
    }

    private NamedType resolveAutoValue(NamedType namedType) {
        final Class<?> cls = namedType.getType();
        if (cls.getSimpleName().startsWith("AutoValue_")) {
            return new NamedType(cls.getSuperclass(), namedType.getName());
        } else {
            return namedType;
        }
    }
}
