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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RuntimeTunableConditions {
    private RuntimeTunableConditions() {
    }

    public static Set<String> inactivePaths(JsonNode schema, JsonNode value) {
        final Set<String> result = new HashSet<>();
        for (Map.Entry<String, JsonNode> property : schema.path("properties").properties()) {
            final JsonNode propertySchema = property.getValue();
            if (!matches(propertySchema.path("x-graylog-enabled-when"), value)
                    || !matches(propertySchema.path("x-graylog-visible-when"), value)) {
                result.add(property.getKey());
            }
        }
        return result;
    }

    private static boolean matches(JsonNode conditions, JsonNode siblings) {
        for (JsonNode condition : conditions) {
            if (!matchesCondition(condition, siblings)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesCondition(JsonNode condition, JsonNode siblings) {
        final JsonNode sibling = siblings.path(condition.path("property").asText());
        final String text = sibling.isNull() || sibling.isMissingNode() ? "null" : sibling.asText();
        for (JsonNode allowed : condition.path("in")) {
            if (allowed.asText().equals(text)) {
                return true;
            }
        }
        return false;
    }
}
