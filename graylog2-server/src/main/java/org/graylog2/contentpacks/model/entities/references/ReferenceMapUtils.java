/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.model.entities.references;

import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.model.parameters.FilledParameter;

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
        // TODO: Support nested objects in list
        return list.stream()
                .map(ValueReference::of)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ReferenceList::new));
    }

    public static Map<String, Object> toValueMap(ReferenceMap m, Map<String, FilledParameter<?>> parameters) {
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
    private static Object valueOf(Reference value, Map<String, FilledParameter<?>> parameters) {
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

    private static Object valueOf(ValueReference valueReference, Map<String, FilledParameter<?>> parameters) {
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
                // TODO: Resolve parameter
            default:
                return null;
        }
    }

    private static List<Object> toValueList(ReferenceList list, Map<String, FilledParameter<?>> parameters) {
        return list.stream()
                .map(valueReference -> valueOf(valueReference, parameters))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
