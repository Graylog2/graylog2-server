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
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupResultTest {
    private static final ImmutableMap<Object, Object> MULTI_VALUE = ImmutableMap.of(
            "int", 42,
            "bool", true,
            "string", "Foobar"
    );

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void serializeEmpty() {
        final LookupResult lookupResult = LookupResult.empty();
        final JsonNode node = objectMapper.convertValue(lookupResult, JsonNode.class);

        assertThat(node.isNull()).isFalse();
        assertThat(node.path("single_value").isNull()).isTrue();
        assertThat(node.path("multi_value").isNull()).isTrue();
        assertThat(node.path("ttl").asLong()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void deserializeEmpty() throws IOException {
        final String json = "{\"single_value\":null,\"multi_value\":null,\"ttl\":23}";
        final LookupResult lookupResult = objectMapper.readValue(json, LookupResult.class);

        assertThat(lookupResult.isEmpty()).isTrue();
        assertThat(lookupResult.singleValue()).isNull();
        assertThat(lookupResult.multiValue()).isNull();
        assertThat(lookupResult.cacheTTL()).isEqualTo(23L);
    }


    @Test
    public void serializeSingleNumber() {
        final LookupResult lookupResult = LookupResult.single(42);
        final JsonNode node = objectMapper.convertValue(lookupResult, JsonNode.class);

        assertThat(node.isNull()).isFalse();
        assertThat(node.path("single_value").asInt()).isEqualTo(42);
        assertThat(node.path("multi_value").path("value").asInt()).isEqualTo(42);
        assertThat(node.path("ttl").asLong()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void deserializeSingleNumber() throws IOException {
        final String json = "{\"single_value\":42,\"multi_value\":{\"value\":42},\"ttl\":23}";
        final LookupResult lookupResult = objectMapper.readValue(json, LookupResult.class);

        assertThat(lookupResult.isEmpty()).isFalse();
        assertThat(lookupResult.singleValue()).isEqualTo(42);
        assertThat(lookupResult.multiValue()).hasEntrySatisfying("value", v -> assertThat(v).isEqualTo(42));
        assertThat(lookupResult.cacheTTL()).isEqualTo(23L);
    }

    @Test
    public void serializeSingleBoolean() {
        final LookupResult lookupResult = LookupResult.single(true);
        final JsonNode node = objectMapper.convertValue(lookupResult, JsonNode.class);

        assertThat(node.isNull()).isFalse();
        assertThat(node.path("single_value").asBoolean()).isTrue();
        assertThat(node.path("multi_value").path("value").asBoolean()).isTrue();
        assertThat(node.path("ttl").asLong()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void deserializeSingleBoolean() throws IOException {
        final String json = "{\"single_value\":true,\"multi_value\":{\"value\":true},\"ttl\":23}";
        final LookupResult lookupResult = objectMapper.readValue(json, LookupResult.class);

        assertThat(lookupResult.isEmpty()).isFalse();
        assertThat(lookupResult.singleValue()).isEqualTo(true);
        assertThat(lookupResult.multiValue()).hasEntrySatisfying("value", v -> assertThat(v).isEqualTo(true));
        assertThat(lookupResult.cacheTTL()).isEqualTo(23L);
    }

    @Test
    public void serializeMultiString() {
        final LookupResult lookupResult = LookupResult.multi("Foobar", MULTI_VALUE);
        final JsonNode node = objectMapper.convertValue(lookupResult, JsonNode.class);

        assertThat(node.isNull()).isFalse();
        assertThat(node.path("single_value").asText()).isEqualTo("Foobar");
        assertThat(node.path("multi_value").path("int").asInt()).isEqualTo(42);
        assertThat(node.path("multi_value").path("bool").asBoolean()).isEqualTo(true);
        assertThat(node.path("multi_value").path("string").asText()).isEqualTo("Foobar");
        assertThat(node.path("ttl").asLong()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void deserializeMultiString() throws IOException {
        final String json = "{\"single_value\":\"Foobar\",\"multi_value\":{\"int\":42,\"bool\":true,\"string\":\"Foobar\"},\"ttl\":23}";
        final LookupResult lookupResult = objectMapper.readValue(json, LookupResult.class);

        assertThat(lookupResult.isEmpty()).isFalse();
        assertThat(lookupResult.singleValue()).isEqualTo("Foobar");
        assertThat(lookupResult.multiValue()).isEqualTo(MULTI_VALUE);
        assertThat(lookupResult.cacheTTL()).isEqualTo(23L);
    }

    @Test
    public void serializeMultiNumber() {
        final LookupResult lookupResult = LookupResult.multi(42, MULTI_VALUE);
        final JsonNode node = objectMapper.convertValue(lookupResult, JsonNode.class);

        assertThat(node.isNull()).isFalse();
        assertThat(node.path("single_value").asInt()).isEqualTo(42);
        assertThat(node.path("multi_value").path("int").asInt()).isEqualTo(42);
        assertThat(node.path("multi_value").path("bool").asBoolean()).isEqualTo(true);
        assertThat(node.path("multi_value").path("string").asText()).isEqualTo("Foobar");
        assertThat(node.path("ttl").asLong()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void deserializeMultiNumber() throws IOException {
        final String json = "{\"single_value\":42,\"multi_value\":{\"int\":42,\"bool\":true,\"string\":\"Foobar\"},\"ttl\":23}";
        final LookupResult lookupResult = objectMapper.readValue(json, LookupResult.class);

        assertThat(lookupResult.isEmpty()).isFalse();
        assertThat(lookupResult.singleValue()).isEqualTo(42);
        assertThat(lookupResult.multiValue()).isEqualTo(MULTI_VALUE);
        assertThat(lookupResult.cacheTTL()).isEqualTo(23L);
    }

    @Test
    public void serializeMultiBoolean() {
        final LookupResult lookupResult = LookupResult.multi(true, MULTI_VALUE);
        final JsonNode node = objectMapper.convertValue(lookupResult, JsonNode.class);

        assertThat(node.isNull()).isFalse();
        assertThat(node.path("single_value").asBoolean()).isTrue();
        assertThat(node.path("multi_value").path("int").asInt()).isEqualTo(42);
        assertThat(node.path("multi_value").path("bool").asBoolean()).isEqualTo(true);
        assertThat(node.path("multi_value").path("string").asText()).isEqualTo("Foobar");
        assertThat(node.path("ttl").asLong()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void deserializeMultiBoolean() throws IOException {
        final String json = "{\"single_value\":true,\"multi_value\":{\"int\":42,\"bool\":true,\"string\":\"Foobar\"},\"ttl\":23}";
        final LookupResult lookupResult = objectMapper.readValue(json, LookupResult.class);

        assertThat(lookupResult.isEmpty()).isFalse();
        assertThat(lookupResult.singleValue()).isEqualTo(true);
        assertThat(lookupResult.multiValue()).isEqualTo(MULTI_VALUE);
        assertThat(lookupResult.cacheTTL()).isEqualTo(23L);
    }
}