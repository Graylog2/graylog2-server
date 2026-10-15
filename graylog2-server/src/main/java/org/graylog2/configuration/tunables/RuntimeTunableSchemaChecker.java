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
package org.graylog2.configuration.tunables;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

final class RuntimeTunableSchemaChecker {
    private static final Set<String> SCALAR_TYPES = Set.of("boolean", "integer", "number", "string");
    private static final List<String> CONDITION_KEYS = List.of("x-graylog-enabled-when");

    private RuntimeTunableSchemaChecker() {
    }

    static void normalizeAndCheck(ObjectNode root) throws UnsupportedTunableSchemaException {
        if (!types(root).contains("object") || !root.path("properties").isObject()) {
            throw new UnsupportedTunableSchemaException("A runtime tunable must be an object with properties");
        }
        final ObjectNode properties = (ObjectNode) root.get("properties");
        final ArrayNode required = root.putArray("required");
        root.put("additionalProperties", false);
        for (Map.Entry<String, JsonNode> entry : properties.properties()) {
            final ObjectNode property = (ObjectNode) entry.getValue();
            checkConditions(property, properties, entry.getKey());
            checkScalar(property, entry.getKey());
            required.add(entry.getKey());
        }
    }

    private static void checkScalar(ObjectNode node, String name) throws UnsupportedTunableSchemaException {
        removeEmptyObjectMarker(node);
        if (node.has("x-graylog-unsupported")) {
            throw new UnsupportedTunableSchemaException(f("\"%s\": %s", name, node.get("x-graylog-unsupported").asText()));
        }
        JsonNode value = node;
        if (node.has("anyOf")) {
            final List<JsonNode> nonNull = new ArrayList<>();
            node.get("anyOf").forEach(branch -> {
                if (!types(branch).equals(Set.of("null"))) {
                    nonNull.add(branch);
                }
            });
            if (nonNull.size() != 1) {
                throw new UnsupportedTunableSchemaException(f("\"%s\" has an unsupported union type", name));
            }
            value = nonNull.get(0);
        }
        final Set<String> types = new HashSet<>(types(value));
        types.remove("null");
        if (types.size() != 1 || !SCALAR_TYPES.contains(types.iterator().next()) || value.has("oneOf")) {
            throw new UnsupportedTunableSchemaException(f("\"%s\" must be a boolean, number, string, enum or duration, but is: %s", name, node));
        }
    }

    /**
     * The shared schema config marks type-less nodes as empty objects, which is wrong next to anyOf.
     */
    private static void removeEmptyObjectMarker(ObjectNode node) {
        if (node.has("anyOf")
                && "object".equals(node.path("type").asText())
                && node.path("properties").isEmpty()) {
            node.remove("type");
            node.remove("properties");
        }
    }

    private static void checkConditions(ObjectNode property, ObjectNode siblings, String name) throws UnsupportedTunableSchemaException {
        for (String key : CONDITION_KEYS) {
            for (JsonNode condition : property.path(key)) {
                final String target = condition.path("property").asText();
                if (target.equals(name) || !siblings.has(target)) {
                    throw new UnsupportedTunableSchemaException(f("Condition on \"%s\" refers to unknown sibling property \"%s\"", name, target));
                }
            }
        }
    }

    private static Set<String> types(JsonNode node) {
        final JsonNode type = node.path("type");
        if (type.isTextual()) {
            return Set.of(type.asText());
        }
        final Set<String> result = new HashSet<>();
        type.forEach(t -> result.add(t.asText()));
        return result;
    }
}
