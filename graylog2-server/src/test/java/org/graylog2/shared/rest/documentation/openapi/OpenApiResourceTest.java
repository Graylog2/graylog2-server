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
package org.graylog2.shared.rest.documentation.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.graylog2.rest.URIHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiResourceTest {

    @Mock
    private OpenAPIContextFactory contextFactory;

    @Mock
    private OpenApiContext openApiContext;

    private OpenApiResource resource;
    private OpenAPI cachedOpenAPI;

    @BeforeEach
    void setUp() {
        cachedOpenAPI = new OpenAPI()
                .info(new Info().title("Test API").version("1.0.0"))
                .addServersItem(new Server().url("/api/"));

        when(contextFactory.getOrCreate(OpenApiContext.OPENAPI_CONTEXT_ID_DEFAULT)).thenReturn(openApiContext);
        when(openApiContext.read()).thenReturn(cachedOpenAPI);
        lenient().when(openApiContext.getOutputJsonMapper()).thenReturn(Json31.mapper());
        lenient().when(openApiContext.getOutputYamlMapper()).thenReturn(Yaml31.mapper());

        resource = new OpenApiResource(contextFactory);
    }

    private static URIHelper uriHelper(String baseUri) {
        return new URIHelper(URI.create(baseUri));
    }

    @Test
    void setsAbsoluteServerUrlFromOverrideHeader() throws Exception {
        final var response = resource.getOpenApi(uriHelper("https://example.com/graylog/"), null);
        final var json = response.getEntity().toString();
        final var tree = new ObjectMapper().readTree(json);

        assertThat(tree.at("/servers/0/url").asText()).isEqualTo("https://example.com/graylog/api/");
    }

    @Test
    void usesProvidedBaseUri() throws Exception {
        final var response = resource.getOpenApi(uriHelper("https://external.example.com/"), null);
        final var json = response.getEntity().toString();
        final var tree = new ObjectMapper().readTree(json);

        assertThat(tree.at("/servers/0/url").asText()).isEqualTo("https://external.example.com/api/");
    }

    @Test
    void doesNotMutateCachedOpenAPIModel() throws Exception {
        resource.getOpenApi(uriHelper("https://example.com/graylog/"), null);

        // The cached model should still have the original /api/ server URL
        assertThat(cachedOpenAPI.getServers()).hasSize(1);
        assertThat(cachedOpenAPI.getServers().get(0).getUrl()).isEqualTo("/api/");
    }

    @Test
    void returnsYamlWhenExtIsYaml() throws Exception {
        final var response = resource.getOpenApi(uriHelper("http://localhost:9000/"), ".yaml");
        final var yaml = response.getEntity().toString();

        // YAML should not start with '{' (that would be JSON)
        assertThat(yaml).doesNotStartWith("{");
        // Should contain the server URL
        assertThat(yaml).contains("http://localhost:9000/api/");
        // YAML output should be pretty-printed (contains newlines and indentation)
        assertThat(yaml).contains("\n");
    }

    @Test
    void returnsJsonWhenExtIsNull() throws Exception {
        final var response = resource.getOpenApi(uriHelper("http://localhost:9000/"), null);
        final var json = response.getEntity().toString();

        // Verify it's valid JSON by parsing it
        final var tree = new ObjectMapper().readTree(json);
        assertThat(tree.has("openapi")).isTrue();
    }

    @Test
    void returns404WhenOpenAPIModelIsNull() throws Exception {
        when(openApiContext.read()).thenReturn(null);

        final var response = resource.getOpenApi(uriHelper("http://localhost:9000/"), null);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void prettyPrintsJsonOutput() throws Exception {
        final var response = resource.getOpenApi(uriHelper("http://localhost:9000/"), null);
        final var json = response.getEntity().toString();

        // Pretty-printed JSON contains newlines and indentation
        assertThat(json).contains("\n");
        assertThat(json).contains("  ");
    }
}
