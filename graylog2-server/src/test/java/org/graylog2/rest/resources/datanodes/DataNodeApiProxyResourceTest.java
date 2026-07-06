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
package org.graylog2.rest.resources.datanodes;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNodeApiProxyResourceTest {

    private static final ProxyRequestAdapter.ProxyResponse OK_RESPONSE =
            new ProxyRequestAdapter.ProxyResponse(200, InputStream.nullInputStream(), "application/json");

    @Mock ContainerRequestContext requestContext;
    @Mock UriInfo uriInfo;

    @BeforeEach
    void setUp() {
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getEntityStream()).thenReturn(InputStream.nullInputStream());
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
    }

    private DataNodeApiProxyResource resource(ProxyRequestAdapter adapter, boolean allowlist) {
        return new DataNodeApiProxyResource(adapter, allowlist);
    }

    // --- Allowlist disabled ---

    @Test
    void allowlistDisabledForwardsAnyPath() throws IOException {
        final var adapter = new RecordingAdapter(OK_RESPONSE);
        final Response response = resource(adapter, false).requestGet("_nodes/stats", "myhost", requestContext);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(adapter.lastRequest.path()).isEqualTo("_nodes/stats");
    }

    // --- Allowlist enabled: permitted paths ---

    @ParameterizedTest
    @ValueSource(strings = {"_cluster/health", "_cluster/stats"})
    void allowlistPermitsClusterPaths(String path) throws IOException {
        final var adapter = new RecordingAdapter(OK_RESPONSE);
        assertThat(resource(adapter, true).requestGet(path, "myhost", requestContext).getStatus()).isEqualTo(200);
        assertThat(adapter.lastRequest).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"_cat/indices", "_cat/nodes"})
    void allowlistPermitsCatPaths(String path) throws IOException {
        final var adapter = new RecordingAdapter(OK_RESPONSE);
        assertThat(resource(adapter, true).requestGet(path, "myhost", requestContext).getStatus()).isEqualTo(200);
        assertThat(adapter.lastRequest).isNotNull();
    }

    @Test
    void allowlistPermitsMappingGet() throws IOException {
        final var adapter = new RecordingAdapter(OK_RESPONSE);
        assertThat(resource(adapter, true).requestGet("_mapping/myindex", "myhost", requestContext).getStatus()).isEqualTo(200);
        assertThat(adapter.lastRequest).isNotNull();
    }

    // --- Allowlist enabled: blocked paths ---

    @Test
    void allowlistBlocksArbitraryPath() throws IOException {
        final var adapter = new RecordingAdapter(null);
        final Response response = resource(adapter, true).requestGet("_nodes/stats", "myhost", requestContext);
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(adapter.lastRequest).isNull();
    }

    @Test
    void allowlistBlocksMappingPost() throws IOException {
        when(requestContext.getMethod()).thenReturn("POST");
        final var adapter = new RecordingAdapter(null);
        final Response response = resource(adapter, true).requestPost("_mapping/myindex", "myhost", requestContext);
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(adapter.lastRequest).isNull();
    }

    // --- Response forwarding ---

    @Test
    void responseStatusIsForwarded() throws IOException {
        final var adapter = new RecordingAdapter(new ProxyRequestAdapter.ProxyResponse(404, InputStream.nullInputStream(), "application/json"));
        assertThat(resource(adapter, false).requestGet("_cluster/health", "myhost", requestContext).getStatus()).isEqualTo(404);
    }

    @Test
    void responseContentTypeIsForwarded() throws IOException {
        final var adapter = new RecordingAdapter(new ProxyRequestAdapter.ProxyResponse(200, InputStream.nullInputStream(), "text/plain"));
        assertThat(resource(adapter, false).requestGet("_cluster/health", "myhost", requestContext).getMediaType().toString()).isEqualTo("text/plain");
    }

    @Test
    void hostnameIsPassedToAdapter() throws IOException {
        final var adapter = new RecordingAdapter(OK_RESPONSE);
        resource(adapter, false).requestGet("_cluster/health", "my-datanode-host", requestContext);
        assertThat(adapter.lastRequest.hostname()).isEqualTo("my-datanode-host");
    }

    @Test
    void queryParametersArePassedToAdapter() throws IOException {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("v", "true");
        when(uriInfo.getQueryParameters()).thenReturn(params);
        final var adapter = new RecordingAdapter(OK_RESPONSE);
        resource(adapter, false).requestGet("_cat/indices", "myhost", requestContext);
        assertThat(adapter.lastRequest.queryParameters()).isEqualTo(params);
    }

    static class RecordingAdapter implements ProxyRequestAdapter {
        ProxyRequestAdapter.ProxyRequest lastRequest = null;
        private final ProxyRequestAdapter.ProxyResponse response;

        RecordingAdapter(ProxyRequestAdapter.ProxyResponse response) {
            this.response = response;
        }

        @Override
        public ProxyRequestAdapter.ProxyResponse request(ProxyRequestAdapter.ProxyRequest request) {
            this.lastRequest = request;
            if (response == null) {
                throw new IllegalStateException("Adapter should not have been called for: " + request.path());
            }
            return response;
        }
    }
}
