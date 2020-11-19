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
package org.graylog2.indexer.searches;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SearchFailure {

    final private List<String> errors;

    public SearchFailure(JsonNode shards) {
        errors = StreamSupport.stream(shards.path("failures").spliterator(), false)
                .map(failure -> {
                    final String error = failure.path("reason").path("reason").asText();
                    final String caused_by = failure.path("reason").path("caused_by").toString();
                    if (!caused_by.isEmpty()) {
                        return error + " caused_by: " + caused_by;
                    }
                    return error;
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getNonNumericFieldErrors() {
        return errors.stream().filter(error ->
                error.startsWith("Expected numeric type on field") ||
                error.contains("\"type\":\"number_format_exception")).
                collect(Collectors.toList());
    }
}
