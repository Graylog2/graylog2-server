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
package org.graylog2.plugin.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationResultTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void plainErrorsHaveNoDetails() {
        final JsonNode json = objectMapper.valueToTree(new ValidationResult().addError("title", "Title is required"));

        assertThat(json.get("failed").asBoolean()).isTrue();
        assertThat(json.get("errors").get("title").get(0).asText()).isEqualTo("Title is required");
        assertThat(json.has("error_details")).isFalse();
    }

    @Test
    void codedErrorKeepsMessageAndAddsDetails() {
        final JsonNode json = objectMapper.valueToTree(new ValidationResult()
                .addError("title", "event_definition.title_missing", "Title \"{x}\" is required", Map.of("x", "a")));

        assertThat(json.get("errors").get("title").get(0).asText()).isEqualTo("Title \"{x}\" is required");
        final JsonNode detail = json.get("error_details").get("title").get(0);
        assertThat(detail.get("code").asText()).isEqualTo("event_definition.title_missing");
        assertThat(detail.get("message").asText()).isEqualTo("Title \"{x}\" is required");
        assertThat(detail.get("params").get("x").asText()).isEqualTo("a");
    }

    @Test
    void addAllMergesDetails() {
        final ValidationResult result = new ValidationResult().addAll(
                new ValidationResult().addError("a", "x.y", "Message", Map.of()));

        assertThat(result.getErrorDetails()).containsKey("a");
    }
}
