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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.graylog2.configuration.HttpConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiResourceTest {

    @Mock
    private OpenAPIContextFactory contextFactory;

    @Mock
    private OpenApiContext openApiContext;

    @Mock
    private HttpConfiguration httpConfiguration;

    @Mock
    private HttpHeaders httpHeaders;

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
        lenient().when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://localhost:9000/"));

        resource = new OpenApiResource(contextFactory, httpConfiguration);
    }

    @Test
    void setsAbsoluteServerUrlFromOverrideHeader() throws Exception {
        final var headers = new MultivaluedHashMap<String, String>();
        headers.put(HttpConfiguration.OVERRIDE_HEADER, List.of("https://example.com/graylog/"));
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);

        final var response = resource.getOpenApi(httpHeaders, null);
        final var json = response.getEntity().toString();
        final var tree = new ObjectMapper().readTree(json);

        assertThat(tree.at("/servers/0/url").asText()).isEqualTo("https://example.com/graylog/api/");
    }

    @Test
    void fallsBackToExternalUriWhenNoHeader() throws Exception {
        final var headers = new MultivaluedHashMap<String, String>();
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://external.example.com/"));

        final var response = resource.getOpenApi(httpHeaders, null);
        final var json = response.getEntity().toString();
        final var tree = new ObjectMapper().readTree(json);

        assertThat(tree.at("/servers/0/url").asText()).isEqualTo("https://external.example.com/api/");
    }

    @Test
    void fallsBackToPublishUriWhenNoHeaderOrExternalUri() throws Exception {
        final var headers = new MultivaluedHashMap<String, String>();
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://10.0.0.1:9000/"));

        final var response = resource.getOpenApi(httpHeaders, null);
        final var json = response.getEntity().toString();
        final var tree = new ObjectMapper().readTree(json);

        assertThat(tree.at("/servers/0/url").asText()).isEqualTo("http://10.0.0.1:9000/api/");
    }

    @Test
    void doesNotMutateCachedOpenAPIModel() throws Exception {
        final var headers = new MultivaluedHashMap<String, String>();
        headers.put(HttpConfiguration.OVERRIDE_HEADER, List.of("https://example.com/graylog/"));
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);

        resource.getOpenApi(httpHeaders, null);

        // The cached model should still have the original /api/ server URL
        assertThat(cachedOpenAPI.getServers()).hasSize(1);
        assertThat(cachedOpenAPI.getServers().get(0).getUrl()).isEqualTo("/api/");
    }

    @Test
    void returnsYamlWhenExtIsYaml() throws Exception {
        final var headers = new MultivaluedHashMap<String, String>();
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://localhost:9000/"));

        final var response = resource.getOpenApi(httpHeaders, ".yaml");
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
        final var headers = new MultivaluedHashMap<String, String>();
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://localhost:9000/"));

        final var response = resource.getOpenApi(httpHeaders, null);
        final var json = response.getEntity().toString();

        // Verify it's valid JSON by parsing it
        final var tree = new ObjectMapper().readTree(json);
        assertThat(tree.has("openapi")).isTrue();
    }

    @Test
    void returns404WhenOpenAPIModelIsNull() throws Exception {
        when(openApiContext.read()).thenReturn(null);

        final var response = resource.getOpenApi(httpHeaders, null);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void prettyPrintsJsonOutput() throws Exception {
        final var headers = new MultivaluedHashMap<String, String>();
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://localhost:9000/"));

        final var response = resource.getOpenApi(httpHeaders, null);
        final var json = response.getEntity().toString();

        // Pretty-printed JSON contains newlines and indentation
        assertThat(json).contains("\n");
        assertThat(json).contains("  ");
    }
}
