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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One row of {@code _cat/indices?format=json&bytes=b}. OpenSearch returns every column as a string;
 * closed indices have no health, docs or size.
 */
public record CatIndex(String index,
                       String health,
                       String status,
                       Integer primaryShards,
                       Integer replicas,
                       Long docsCount,
                       Long storeSizeBytes) {

    static CatIndex fromJson(JsonNode row) {
        return new CatIndex(
                text(row, "index"),
                text(row, "health"),
                text(row, "status"),
                integer(row, "pri"),
                integer(row, "rep"),
                number(row, "docs.count"),
                number(row, "store.size"));
    }

    private static String text(JsonNode row, String field) {
        final JsonNode value = row.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Long number(JsonNode row, String field) {
        final String value = text(row, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer integer(JsonNode row, String field) {
        final Long value = number(row, field);
        return value == null ? null : value.intValue();
    }
}
