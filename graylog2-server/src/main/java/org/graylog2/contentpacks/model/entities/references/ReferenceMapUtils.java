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
package org.graylog2.contentpacks.model.entities.references;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ReferenceMapUtils {
    public static ReferenceMap toReferenceMap(Map<String, Object> m) {
        final ImmutableMap.Builder<String, Reference> mapBuilder = ImmutableMap.builder();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof Collection) {
                @SuppressWarnings("unchecked") final List<Object> childList = (List<Object>) value;
                mapBuilder.put(entry.getKey(), toReferenceList(childList));
            }
            if (value instanceof Map) {
                @SuppressWarnings("unchecked") final Map<String, Object> childMap = (Map<String, Object>) value;
                mapBuilder.put(entry.getKey(), toReferenceMap(childMap));
            } else {
                final ValueReference valueReference = ValueReference.of(value);
                if (valueReference != null) {
                    mapBuilder.put(entry.getKey(), valueReference);
                }
            }
        }
        return new ReferenceMap(mapBuilder.build());
    }

    private static ReferenceList toReferenceList(Collection<Object> list) {
        if (list.size() == 0) {
            return new ReferenceList();
        }

        if (list.iterator().next() instanceof Map) {
            return list.stream()
                    .map(r -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> value = (Map) r;
                        return ReferenceMapUtils.toReferenceMap(value);
                    })
                    .collect(Collectors.toCollection(ReferenceList::new));
        }

        return list.stream()
                .map(ValueReference::of)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ReferenceList::new));
    }

    public static Map<String, Object> toValueMap(ReferenceMap m, Map<String, ValueReference> parameters) {
        final ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.builder();
        for (Map.Entry<String, Reference> entry : m.entrySet()) {
            final Object value = valueOf(entry.getValue(), parameters);
            if (value != null) {
                mapBuilder.put(entry.getKey(), value);
            }
        }
        return mapBuilder.build();
    }

    @Nullable
    private static Object valueOf(Reference value, Map<String, ValueReference> parameters) {
        if (value instanceof ValueReference) {
            final ValueReference valueReference = (ValueReference) value;
            return valueOf(valueReference, parameters);
        } else if (value instanceof ReferenceList) {
            @SuppressWarnings("unchecked") final ReferenceList collection = (ReferenceList) value;
            return toValueList(collection, parameters);
        } else if (value instanceof ReferenceMap) {
            @SuppressWarnings("unchecked") final ReferenceMap map = (ReferenceMap) value;
            return toValueMap(map, parameters);
        } else {
            return null;
        }
    }

    @Nullable
    private static Object valueOf(ValueReference valueReference, Map<String, ValueReference> parameters) {
        switch (valueReference.valueType()) {
            case BOOLEAN:
                return valueReference.asBoolean(parameters);
            case DOUBLE:
                return valueReference.asDouble(parameters);
            case FLOAT:
                return valueReference.asFloat(parameters);
            case INTEGER:
                return valueReference.asInteger(parameters);
            case LONG:
                return valueReference.asLong(parameters);
            case STRING:
                return valueReference.asString(parameters);
            case PARAMETER:
                return resolveParameterReference(valueReference, parameters);
            default:
                return null;
        }
    }

    @Nullable
    private static Object resolveParameterReference(ValueReference valueReference,
                                                    Map<String, ValueReference> parameters) {
        final Object parameterName = valueReference.value();
        if (parameterName instanceof String) {
            final ValueReference resolvedParameter = parameters.get(parameterName);
            if (resolvedParameter == null) {
                // TODO: Create custom exception?
                throw new IllegalArgumentException("Missing parameter " + parameterName);
            }
            if (resolvedParameter.valueType() == ValueType.PARAMETER) {
                // TODO: Create custom exception?
                throw new IllegalArgumentException("Circular parameter " + parameterName);
            }
            return valueOf(resolvedParameter, parameters);
        } else {
            return null;
        }
    }

    private static List<Object> toValueList(ReferenceList list, Map<String, ValueReference> parameters) {
        return list.stream()
                .map(valueReference -> valueOf(valueReference, parameters))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
