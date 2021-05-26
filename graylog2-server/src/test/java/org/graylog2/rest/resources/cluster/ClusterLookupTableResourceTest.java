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
package org.graylog2.rest.resources.cluster;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.system.RemoteLookupTableResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import retrofit2.Call;
import retrofit2.Response;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterLookupTableResourceTest {

    @Mock
    NodeService nodeService;

    @Mock
    RemoteInterfaceProvider remoteInterfaceProvider;

    @Mock
    HttpHeaders httpHeaders;

    @Mock
    Node node1;

    @Mock
    Node node2;

    @Mock
    RemoteLookupTableResource remoteLookupTableResource1;

    @Mock
    RemoteLookupTableResource remoteLookupTableResource2;

    private ClusterLookupTableResource underTest;

    @Before
    public void setup() throws Exception {

        when(httpHeaders.getRequestHeader("Authorization"))
                .thenReturn(Collections.singletonList("TEST_TOKEN"));

        when(nodeService.byNodeId("node_1")).thenReturn(node1);
        when(nodeService.byNodeId("node_2")).thenReturn(node2);

        when(remoteInterfaceProvider.get(node1, "TEST_TOKEN", RemoteLookupTableResource.class))
                .thenReturn(remoteLookupTableResource1);

        when(remoteInterfaceProvider.get(node2, "TEST_TOKEN", RemoteLookupTableResource.class))
                .thenReturn(remoteLookupTableResource2);

        underTest = new ClusterLookupTableResource(nodeService, remoteInterfaceProvider, httpHeaders,
                Executors.newFixedThreadPool(2,
                        new ThreadFactoryBuilder()
                                .setNameFormat("proxied-requests-test-pool-%d")
                                .build()
                ));
    }

    @Test
    public void performPurge_whenAllCallsSucceedThenResponseWithPerNodeDetailsReturned() throws Exception {
        // given
        when(nodeService.allActive()).thenReturn(nodeMap());
        mock204Response("testName", "testKey", remoteLookupTableResource1);
        mock204Response("testName", "testKey", remoteLookupTableResource2);

        // when
        final Map<String, ProxiedResource.CallResult<Void>> res = underTest.performPurge("testName", "testKey");

        // then
        verify(remoteLookupTableResource1).performPurge("testName", "testKey");
        verify(remoteLookupTableResource2).performPurge("testName", "testKey");

        assertThat(res.get("node_1")).satisfies(nodeRes -> {
            assertThat(nodeRes.isSuccess()).isTrue();
            assertThat(nodeRes.serverErrorMessage()).isNull();
            assertThat(nodeRes.response().code()).isEqualTo(204);
            assertThat(nodeRes.response().entity()).isEmpty();
            assertThat(nodeRes.response().errorText()).isNull();
        });

        assertThat(res.get("node_2")).satisfies(nodeRes -> {
            assertThat(nodeRes.isSuccess()).isTrue();
            assertThat(nodeRes.serverErrorMessage()).isNull();
            assertThat(nodeRes.response().code()).isEqualTo(204);
            assertThat(nodeRes.response().entity()).isEmpty();
            assertThat(nodeRes.response().errorText()).isNull();
        });
    }

    @Test
    public void performPurge_whenOneHttpCallFailsThenResponseWithPerNodeDetailsReturned() throws Exception {
        // given
        when(nodeService.allActive()).thenReturn(nodeMap());
        mock204Response("testName", "testKey", remoteLookupTableResource1);
        mock404Response("testName", "testKey", remoteLookupTableResource2);

        // when
        final Map<String, ProxiedResource.CallResult<Void>> res = underTest.performPurge("testName", "testKey");

        // then
        verify(remoteLookupTableResource1).performPurge("testName", "testKey");
        verify(remoteLookupTableResource2).performPurge("testName", "testKey");

        assertThat(res.get("node_1")).satisfies(nodeRes -> {
            assertThat(nodeRes.isSuccess()).isTrue();
            assertThat(nodeRes.serverErrorMessage()).isNull();
            assertThat(nodeRes.response().code()).isEqualTo(204);
            assertThat(nodeRes.response().entity()).isEmpty();
            assertThat(nodeRes.response().errorText()).isNull();

        });
        assertThat(res.get("node_2")).satisfies(nodeRes -> {
            assertThat(nodeRes.isSuccess()).isTrue();
            assertThat(nodeRes.serverErrorMessage()).isNull();
            assertThat(nodeRes.response().code()).isEqualTo(404);
            assertThat(nodeRes.response().entity()).isEmpty();
            assertThat(nodeRes.response().errorText()).isEqualTo("Resource Not Found");
        });
    }

    @Test
    public void performPurge_whenOneCallFailsWithServerErrorThenResponseWithPerNodeDetailsReturned() throws Exception {
        // given
        when(nodeService.allActive()).thenReturn(nodeMap());
        mock204Response("testName", "testKey", remoteLookupTableResource1);
        when(remoteLookupTableResource2.performPurge(anyString(), anyString()))
                .thenThrow(new RuntimeException("Some exception"));

        // when
        final Map<String, ProxiedResource.CallResult<Void>> res = underTest.performPurge("testName", "testKey");

        // then
        verify(remoteLookupTableResource1).performPurge("testName", "testKey");
        verify(remoteLookupTableResource2).performPurge("testName", "testKey");

        assertThat(res.get("node_1")).satisfies(nodeRes -> {
            assertThat(nodeRes.isSuccess()).isTrue();
            assertThat(nodeRes.serverErrorMessage()).isNull();
            assertThat(nodeRes.response().code()).isEqualTo(204);
            assertThat(nodeRes.response().entity()).isEmpty();
            assertThat(nodeRes.response().errorText()).isNull();

        });

        assertThat(res.get("node_2")).satisfies(nodeRes -> {
            assertThat(nodeRes.isSuccess()).isFalse();
            assertThat(nodeRes.serverErrorMessage()).isEqualTo("Some exception");
            assertThat(nodeRes.response()).isNull();
        });
    }

    private void mock204Response(String tableName, String key,
                                 RemoteLookupTableResource remoteLookupTableResource) throws IOException {
        final Call<Void> call = mock(Call.class);

        when(remoteLookupTableResource.performPurge(tableName, key))
                .thenReturn(call);

        when(call.execute()).thenReturn(Response.success(null,
                new okhttp3.Response.Builder() //
                        .code(204)
                        .message("No-Content")
                        .protocol(Protocol.HTTP_1_1)
                        .request(new Request.Builder().url("http://localhost/").build())
                        .build()));
    }

    private void mock404Response(String tableName, String key,
                                 RemoteLookupTableResource remoteLookupTableResource) throws IOException {
        final Call<Void> call = mock(Call.class);

        when(remoteLookupTableResource.performPurge(tableName, key))
                .thenReturn(call);

        when(call.execute()).thenReturn(Response.error(
                ResponseBody.create(null, "Resource Not Found"),
                new okhttp3.Response.Builder() //
                        .code(404)
                        .message("Not Found")
                        .protocol(Protocol.HTTP_1_1)
                        .request(new Request.Builder().url("http://localhost/").build())
                        .build()
                )
        );
    }

    private Map<String, Node> nodeMap() {
        final Map<String, Node> nodes = new LinkedHashMap<>();
        nodes.put("node_2", node2);
        nodes.put("node_1", node1);
        return nodes;
    }
}
