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
package org.graylog.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class McpRestResourceTest {

    private static McpRestResource resource;

    @BeforeAll
    public static void setUp() throws Exception {
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        // for this test we really only need the cluster config service, if we want to check more
        // we have to create mocks for the rest, too
        resource = new McpRestResource(clusterConfigService,
                                       null,
                                       null);
        clusterConfigService.write(McpConfiguration.create(false, false, false));
    }

    @Test
    public void postDisabledWithClusterConfig() {
        final Response response = resource.get();
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void getDisabledWithClusterConfig() {
        final Response response = resource.get();
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void pingReturnsEmptyResultInsteadOf500() throws Exception {
        // Regression test for https://github.com/Graylog2/graylog2-server/issues/27066
        // The ping utility yields an empty Optional, which must still serialize as a valid JSON-RPC "result": {}
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        clusterConfigService.write(McpConfiguration.create(true, false, false));

        final McpService mcpService = mock(McpService.class);
        final McpSchema.JSONRPCRequest pingRequest =
                new McpSchema.JSONRPCRequest("2.0", McpSchema.METHOD_PING, 2, null);
        when(mcpService.parseMessage(any())).thenReturn(pingRequest);
        when(mcpService.handle(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        final McpRestResource pingResource = new McpRestResource(clusterConfigService, mcpService, mock(SecurityContext.class)) {
            @Override
            protected User getCurrentUser() {
                return mock(User.class);
            }
        };

        final ObjectMapper objectMapper = new ObjectMapper();
        final Response response = pingResource.post(
                MediaType.APPLICATION_JSON,
                ProtocolVersions.MCP_2025_06_18,
                null,
                null,
                objectMapper.createObjectNode()
                        .put("jsonrpc", "2.0")
                        .put("id", 2)
                        .put("method", McpSchema.METHOD_PING));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isInstanceOf(McpSchema.JSONRPCResponse.class);
        final McpSchema.JSONRPCResponse jsonRpcResponse = (McpSchema.JSONRPCResponse) response.getEntity();
        assertThat(jsonRpcResponse.error()).isNull();
        assertThat(jsonRpcResponse.result()).isEqualTo(Map.of());
    }

}
