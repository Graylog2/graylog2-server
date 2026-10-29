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
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Finds tunables whose schema properties differ from the properties Jackson serializes.
 */
final class SchemaJacksonConsistency {
    private SchemaJacksonConsistency() {
    }

    static List<String> mismatches(JacksonProperties jacksonProperties, Class<?> type, JsonNode schema) {
        final Set<String> schemaNames = new TreeSet<>();
        schema.path("properties").fieldNames().forEachRemaining(schemaNames::add);
        final Set<String> jacksonNames = jacksonProperties.properties(type).stream()
                .filter(BeanPropertyDefinition::couldSerialize)
                .map(BeanPropertyDefinition::getName)
                .collect(Collectors.toCollection(TreeSet::new));
        if (schemaNames.equals(jacksonNames)) {
            return List.of();
        }
        return List.of(f("%s: schema has %s, Jackson writes %s", type.getSimpleName(), schemaNames, jacksonNames));
    }
}
