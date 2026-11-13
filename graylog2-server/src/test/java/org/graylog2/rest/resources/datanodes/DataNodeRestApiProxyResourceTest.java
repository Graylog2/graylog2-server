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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.subject.Subject;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataNodeRestApiProxyResourceTest {

    @Mock
    DatanodeRestApiProxy proxy;
    @Mock
    ContainerRequestContext requestContext;
    @Mock
    UriInfo uriInfo;
    @Mock
    Subject subject;

    @BeforeEach
    void setUp() throws IOException {
        when(requestContext.getEntityStream()).thenReturn(InputStream.nullInputStream());
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(proxy.request(any())).thenReturn(new ProxyRequestAdapter.ProxyResponse(200, InputStream.nullInputStream(), "application/json"));
    }

    private DataNodeRestApiProxyResource resource(boolean allowlist) {
        return new DataNodeRestApiProxyResource(proxy, allowlist) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    private Response call(DataNodeRestApiProxyResource resource, String method, String path) throws IOException {
        when(requestContext.getMethod()).thenReturn(method);
        return switch (method) {
            case "GET" -> resource.requestGet(path, "datanode1", requestContext);
            case "POST" -> resource.requestPost(path, "datanode1", requestContext);
            case "PUT" -> resource.requestPut(path, "datanode1", requestContext);
            case "DELETE" -> resource.requestDelete(path, "datanode1", requestContext);
            default -> throw new IllegalArgumentException(method);
        };
    }

    private void grant(String permission) {
        when(subject.isPermitted(permission)).thenReturn(true);
    }

    private void assertForwarded(Response response, String expectedPath) throws IOException {
        assertThat(response.getStatus()).isEqualTo(200);
        final ArgumentCaptor<ProxyRequestAdapter.ProxyRequest> captor = ArgumentCaptor.forClass(ProxyRequestAdapter.ProxyRequest.class);
        verify(proxy).request(captor.capture());
        assertThat(captor.getValue().path()).isEqualTo(expectedPath);
    }

    private void assertRejected(Response response) throws IOException {
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(proxy, never()).request(any());
    }

    @ParameterizedTest
    @CsvSource({
            "GET, logs/stdout, " + RestPermissions.CLUSTER_CONFIGURATION_READ,
            "GET, logs/stderr, " + RestPermissions.CLUSTER_CONFIGURATION_READ,
            "GET, metrics, " + RestPermissions.CLUSTER_CONFIGURATION_READ,
            "POST, metrics/multiple, " + RestPermissions.CLUSTER_CONFIGURATION_READ,
            "GET, indices-directory/compatibility, " + RestPermissions.DATANODE_MIGRATION
    })
    void permitsAllowlistedRequestsWithPermission(String method, String path, String permission) throws IOException {
        grant(permission);
        assertForwarded(call(resource(true), method, path), path);
    }

    @ParameterizedTest
    @CsvSource({
            "GET, logs/stdout",
            "POST, metrics/multiple",
            "GET, indices-directory/compatibility"
    })
    void rejectsAllowlistedPathsWithoutPermission(String method, String path) throws IOException {
        assertRejected(call(resource(true), method, path));
    }

    @Test
    void rejectsPathWhenOnlyPermittedForAnotherRule() throws IOException {
        grant(RestPermissions.CLUSTER_CONFIGURATION_READ);
        assertRejected(call(resource(true), "GET", "indices-directory/compatibility"));
    }

    @ParameterizedTest
    @CsvSource({
            "DELETE, logs/stdout",
            "PUT, metrics",
            "POST, logs/stdout",
            "POST, indices-directory/compatibility"
    })
    void rejectsAllowlistedPathsWithWrongMethod(String method, String path) throws IOException {
        grant(RestPermissions.CLUSTER_CONFIGURATION_READ);
        grant(RestPermissions.DATANODE_MIGRATION);
        assertRejected(call(resource(true), method, path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"logsfoo", "logs_secret/x", "metricsX", "metrics-backup", "indices-directoryX/compatibility"})
    void rejectsPathsSharingOnlyCharactersWithAllowedPrefix(String path) throws IOException {
        grant(RestPermissions.CLUSTER_CONFIGURATION_READ);
        grant(RestPermissions.DATANODE_MIGRATION);
        assertRejected(call(resource(true), "GET", path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"metrics/../management/stop", "logs/../../management/stop", "../management/stop"})
    void rejectsTraversalOutOfAllowedPrefix(String path) throws IOException {
        grant(RestPermissions.CLUSTER_CONFIGURATION_READ);
        assertRejected(call(resource(true), "POST", path));
    }

    @Test
    void forwardsNormalizedPath() throws IOException {
        grant(RestPermissions.CLUSTER_CONFIGURATION_READ);
        assertForwarded(call(resource(true), "GET", "logs/./x/../stdout"), "logs/stdout");
    }

    @Test
    void allowlistDisabledForwardsAnyPathButStillRejectsTraversal() throws IOException {
        assertForwarded(call(resource(false), "POST", "management/stop"), "management/stop");
        assertThat(call(resource(false), "GET", "../secrets").getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
