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
package org.graylog.plugins.pipelineprocessor.compiler;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/** Constants of one generated class. Each becomes a static final field initialized from the hidden class's class data. */
final class RuleClassData {
    private final List<Object> values = new ArrayList<>();
    private final List<Type> types = new ArrayList<>();
    private final IdentityHashMap<Object, Integer> index = new IdentityHashMap<>();

    /** Returns the field index for {@code value}; {@code fieldType} must be visible from the server class loader. */
    int add(Object value, Class<?> fieldType) {
        final Type type = Type.getType(fieldType);
        final Integer existing = index.get(value);
        if (existing != null && types.get(existing).equals(type)) {
            return existing;
        }
        values.add(value);
        types.add(type);
        index.put(value, values.size() - 1);
        return values.size() - 1;
    }

    static String fieldName(int i) {
        return "K" + i;
    }

    int size() {
        return values.size();
    }

    Type type(int i) {
        return types.get(i);
    }

    List<Object> values() {
        return Collections.unmodifiableList(new ArrayList<>(values)); // allows nulls, unlike List.copyOf
    }
}
