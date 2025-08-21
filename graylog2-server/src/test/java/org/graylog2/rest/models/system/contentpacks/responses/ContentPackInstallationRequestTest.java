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
package org.graylog2.rest.models.system.contentpacks.responses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentPackInstallationRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void testSerialisation() {
        final ImmutableMap<String, ValueReference> parameters = ImmutableMap.of(
                "param1", ValueReference.of("string"),
                "param2", ValueReference.of(42),
                "param3", ValueReference.of(3.14d),
                "param4", ValueReference.of(true));
        final ContentPackInstallationRequest request = ContentPackInstallationRequest.create(parameters, "comment");
        final JsonNode node = objectMapper.valueToTree(request);
        assertThat(node.path("comment").asText()).isEqualTo("comment");
        assertThat(node.path("parameters").path("param1").path("@type").asText()).isEqualTo("string");
        assertThat(node.path("parameters").path("param1").path("@value").asText()).isEqualTo("string");
        assertThat(node.path("parameters").path("param2").path("@type").asText()).isEqualTo("integer");
        assertThat(node.path("parameters").path("param2").path("@value").asInt()).isEqualTo(42);
        assertThat(node.path("parameters").path("param3").path("@type").asText()).isEqualTo("double");
        assertThat(node.path("parameters").path("param3").path("@value").asDouble()).isEqualTo(3.14d);
        assertThat(node.path("parameters").path("param4").path("@type").asText()).isEqualTo("boolean");
        assertThat(node.path("parameters").path("param4").path("@value").asBoolean()).isEqualTo(true);
    }

    @Test
    public void testDeserialisation() throws IOException {
        final String json = "{"
                + "\"parameters\":{"
                + "  \"param1\":{\"@type\":\"string\",\"@value\":\"string\"},"
                + "  \"param2\":{\"@type\":\"integer\",\"@value\":42},"
                + "  \"param3\":{\"@type\":\"double\",\"@value\":3.14},"
                + "  \"param4\":{\"@type\":\"boolean\",\"@value\":true}"
                + "},"
                + "\"comment\":\"comment\"" +
                "}";
        final ContentPackInstallationRequest actual = objectMapper.readValue(json, ContentPackInstallationRequest.class);

        final ImmutableMap<String, ValueReference> parameters = ImmutableMap.of(
                "param1", ValueReference.of("string"),
                "param2", ValueReference.of(42),
                "param3", ValueReference.of(3.14d),
                "param4", ValueReference.of(true));
        final ContentPackInstallationRequest expected = ContentPackInstallationRequest.create(parameters, "comment");
        assertThat(actual).isEqualTo(expected);
    }
}
