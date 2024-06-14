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
package org.graylog.plugins.pipelineprocessor.javascript;

import com.google.common.primitives.Ints;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TypeConverter {

    public static <T> T convert(Value value, Class<T> targetType) {
        if (value.isHostObject()) {
            return value.asHostObject();
        }

        if (value.hasArrayElements() && targetType.isAssignableFrom(List.class)) {
            final List<Object> list = new ArrayList<>(Ints.saturatedCast(value.getArraySize()));
            for (int i = 0; i < value.getArraySize(); i++) {
                list.add(convert(value.getArrayElement(i), Object.class));
            }
            return targetType.cast(list);
        }

        if (value.hasMembers() && targetType.isAssignableFrom(Map.class)) {
            final Map<?, ?> map = value.getMemberKeys().stream()
                    .collect(Collectors.toMap(k -> k, k -> convert(value.getMember(k), Object.class)));
            return targetType.cast(map);
        }

        return value.as(targetType);
    }
}
