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
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void plainErrorHasNoCodeAndParams() {
        final JsonNode json = objectMapper.valueToTree(ApiError.create("Something failed"));

        assertThat(json.has("code")).isFalse();
        assertThat(json.has("params")).isFalse();
        assertThat(json.get("message").asText()).isEqualTo("Something failed");
    }

    @Test
    void codedErrorHasCodeAndParams() {
        final JsonNode json = objectMapper.valueToTree(
                ApiError.create("stream.title_exists", "Stream \"Foo\" exists", Map.of("title", "Foo")));

        assertThat(json.get("message").asText()).isEqualTo("Stream \"Foo\" exists");
        assertThat(json.get("code").asText()).isEqualTo("stream.title_exists");
        assertThat(json.get("params").get("title").asText()).isEqualTo("Foo");
    }

    @Test
    void fromCodedExceptionUsesCodeAndParams() {
        final ApiError error = ApiError.from(new CodedBadRequestException("a.b", "English", Map.of("x", 1)));

        assertThat(error.message()).isEqualTo("English");
        assertThat(error.code()).isEqualTo("a.b");
        assertThat(error.params()).containsEntry("x", 1);
    }

    @Test
    void fromPlainExceptionHasNoCode() {
        final ApiError error = ApiError.from(new BadRequestException("English"));

        assertThat(error.message()).isEqualTo("English");
        assertThat(error.code()).isNull();
        assertThat(error.params()).isNull();
    }
}
