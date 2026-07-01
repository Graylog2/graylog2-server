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
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.glassfish.jersey.uri.UriTemplate;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventType;
import org.graylog2.plugin.database.users.User;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpServiceTest {

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private PermissionHelper permissionHelper;

    @Mock
    private User user;

    private ObjectMapper objectMapper;
    private McpService mcpService;
    private Map<String, Tool<?, ?>> tools;
    private Map<GRNType, ResourceProvider> resourceProviders;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tools = new HashMap<>();
        resourceProviders = new HashMap<>();

        when(permissionHelper.getCurrentUser()).thenReturn(user);
        when(user.getName()).thenReturn("testuser");

        mcpService = new McpService(
                objectMapper,
                auditEventSender,
                new CustomizationConfig(null),
                GRNRegistry.createWithBuiltinTypes(),
                tools,
                resourceProviders
        );
    }

    private Optional<McpSchema.Result> handle(McpSchema.JSONRPCRequest request) {
        return mcpService.handle(permissionHelper, request, "session123", null, McpConfiguration.DEFAULT_VALUES);
    }

    @Test
    void testInitializeWithValidProtocolVersion() throws Exception {
        // Given
        var initParams = new McpSchema.InitializeRequest(
                ProtocolVersions.MCP_2025_06_18,
                new McpSchema.ClientCapabilities(null, null, null, null),
                new McpSchema.Implementation("TestClient", "1.0.0")
        );
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_INITIALIZE,
                "1",
                objectMapper.convertValue(initParams, Map.class)
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.InitializeResult.class);

        McpSchema.InitializeResult initResult = (McpSchema.InitializeResult) result.get();
        assertThat(initResult.protocolVersion()).isEqualTo(ProtocolVersions.MCP_2025_06_18);
        assertThat(initResult.serverInfo().name()).isEqualTo("Graylog");
        assertThat(initResult.capabilities()).isNotNull();
        assertThat(initResult.capabilities().prompts()).isNotNull();
        assertThat(initResult.capabilities().resources()).isNotNull();
        assertThat(initResult.capabilities().tools()).isNotNull();

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testInitializeToleratesUnknownCapabilityFields() throws Exception {
        // Regression test for https://github.com/Graylog2/graylog2-server/issues/25956
        // Newer MCP clients may send forward-compatible fields (e.g. capabilities.sampling.tools)
        // that the SDK's Sampling record doesn't declare.
        var params = Map.of(
                "protocolVersion", ProtocolVersions.MCP_2025_06_18,
                "capabilities", Map.of("sampling", Map.of("tools", Map.of())),
                "clientInfo", Map.of("name", "TestClient", "version", "1.0.0")
        );
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_INITIALIZE,
                "1",
                params
        );

        Optional<McpSchema.Result> result = handle(request);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.InitializeResult.class);
        McpSchema.InitializeResult initResult = (McpSchema.InitializeResult) result.get();
        assertThat(initResult.protocolVersion()).isEqualTo(ProtocolVersions.MCP_2025_06_18);
    }

    @Test
    void testInitializeWithInvalidProtocolVersion() {
        // Given
        var initParams = new McpSchema.InitializeRequest(
                "invalid-version",
                new McpSchema.ClientCapabilities(null, null, null, null),
                new McpSchema.Implementation("TestClient", "1.0.0")
        );
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_INITIALIZE,
                "1",
                objectMapper.convertValue(initParams, Map.class)
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.InitializeResult.class);

        McpSchema.InitializeResult initResult = (McpSchema.InitializeResult) result.get();
        assertThat(initResult.protocolVersion()).isEqualTo(McpService.ALL_SUPPORTED_MCP_VERSIONS.getFirst());
        assertThat(initResult.serverInfo().name()).isEqualTo("Graylog");
        assertThat(initResult.capabilities()).isNotNull();
        assertThat(initResult.capabilities().prompts()).isNotNull();
        assertThat(initResult.capabilities().resources()).isNotNull();
        assertThat(initResult.capabilities().tools()).isNotNull();

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testPingReturnsEmpty() throws Exception {
        // Given
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_PING,
                "1",
                null
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testListResources() throws Exception {
        // Given
        ResourceProvider mockProvider = mock(ResourceProvider.class);
        McpSchema.Resource resource1 = McpSchema.Resource.builder()
                .uri("grn::dashboard:test123")
                .name("Test Resource 1")
                .description("Description 1")
                .mimeType("text/plain")
                .build();
        when(mockProvider.list(eq(permissionHelper))).thenReturn(List.of(resource1));

        resourceProviders.put(GRNTypes.DASHBOARD, mockProvider);

        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_RESOURCES_LIST,
                "1",
                null
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.ListResourcesResult.class);

        McpSchema.ListResourcesResult listResult = (McpSchema.ListResourcesResult) result.get();
        assertThat(listResult.resources()).hasSize(1);
        assertThat(listResult.resources().getFirst().uri()).isEqualTo("grn::dashboard:test123");

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testReadResource() throws Exception {
        // Given
        ResourceProvider mockProvider = mock(ResourceProvider.class);
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("grn::dashboard:test123")
                .name("Test Dashboard")
                .description("Dashboard description")
                .mimeType("text/plain")
                .build();
        when(mockProvider.read(eq(permissionHelper), any(URI.class))).thenReturn(Optional.of(resource));

        resourceProviders.put(GRNTypes.DASHBOARD, mockProvider);

        var readParams = new McpSchema.ReadResourceRequest("grn:local:0:internal:dashboard:test123");
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_RESOURCES_READ,
                "1",
                objectMapper.convertValue(readParams, Map.class)
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.ReadResourceResult.class);

        McpSchema.ReadResourceResult readResult = (McpSchema.ReadResourceResult) result.get();
        assertThat(readResult.contents()).hasSize(1);

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testReadResourceThrowsException() {
        // Given
        ResourceProvider mockProvider = mock(ResourceProvider.class);
        when(mockProvider.read(eq(permissionHelper), any(URI.class))).thenReturn(Optional.empty());

        resourceProviders.put(GRNTypes.DASHBOARD, mockProvider);

        var readParams = new McpSchema.ReadResourceRequest("grn:local:0:internal:dashboard:test123");
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_RESOURCES_READ,
                "1",
                objectMapper.convertValue(readParams, Map.class)
        );

        // When/Then
        assertThatThrownBy(() -> handle(request))
                .isInstanceOf(McpError.class)
                .hasMessageContaining("Failed to read resource");
    }

    @Test
    void testReadResourceWithNullDescriptionStillReturnsResource() throws Exception {
        // Regression guard for the MCP SDK 2.0.0 upgrade: the SDK now enforces required fields in its
        // record constructors. McpService builds McpSchema.TextResourceContents(uri, null, description),
        // where the resource description is the (nullable) "text" argument. SDK 2.0.0 rejects a null
        // text with IllegalArgumentException("text must not be null"), which McpService's catch-all maps
        // to RESOURCE_NOT_FOUND. A readable entity with no description (common in Graylog) must not be
        // reported as missing.
        ResourceProvider mockProvider = mock(ResourceProvider.class);
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("grn::dashboard:test123")
                .name("Test Dashboard")
                // intentionally no description -> null
                .build();
        when(mockProvider.read(eq(permissionHelper), any(URI.class))).thenReturn(Optional.of(resource));

        resourceProviders.put(GRNTypes.DASHBOARD, mockProvider);

        var readParams = new McpSchema.ReadResourceRequest("grn:local:0:internal:dashboard:test123");
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_RESOURCES_READ,
                "1",
                objectMapper.convertValue(readParams, Map.class)
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.ReadResourceResult.class);

        McpSchema.ReadResourceResult readResult = (McpSchema.ReadResourceResult) result.get();
        assertThat(readResult.contents()).hasSize(1);

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testListResourceTemplates() throws Exception {
        // Given
        ResourceProvider mockProvider = mock(ResourceProvider.class);
        ResourceProvider.Template template = new ResourceProvider.Template(
                new UriTemplate("grn::dashboard:{id}"),
                "dashboard",
                "Dashboard",
                "Dashboard resources",
                "text/plain"
        );
        when(mockProvider.resourceTemplate()).thenReturn(template);

        resourceProviders.put(GRNTypes.DASHBOARD, mockProvider);

        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_RESOURCES_TEMPLATES_LIST,
                "1",
                null
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.ListResourceTemplatesResult.class);

        McpSchema.ListResourceTemplatesResult listResult = (McpSchema.ListResourceTemplatesResult) result.get();
        assertThat(listResult.resourceTemplates()).hasSize(1);
        assertThat(listResult.resourceTemplates().getFirst().name()).isEqualTo("dashboard");

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testListTools() throws Exception {
        // Given
        Tool<?, ?> mockTool = mock(Tool.class);
        when(mockTool.name()).thenReturn("test_tool");
        when(mockTool.title()).thenReturn("Test Tool");
        when(mockTool.description()).thenReturn("A test tool");
        when(mockTool.inputSchema()).thenReturn(Map.of("type", "object"));

        tools.put("test_tool", mockTool);

        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_TOOLS_LIST,
                "1",
                null
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.ListToolsResult.class);

        McpSchema.ListToolsResult listResult = (McpSchema.ListToolsResult) result.get();
        assertThat(listResult.tools()).hasSize(1);
        assertThat(listResult.tools().getFirst().name()).isEqualTo("test_tool");

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testCallToolWithValidToolName() throws Exception {
        // Given
        Tool<Map<String, Object>, String> mockTool = mock(Tool.class);
        when(mockTool.outputSchema()).thenReturn(Optional.empty());
        when(mockTool.apply(eq(permissionHelper), any())).thenReturn("Tool result");

        tools.put("test_tool", mockTool);

        Map<String, Object> args = Map.of("param1", "value1");
        var callParams = new McpSchema.CallToolRequest("test_tool", args);
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_TOOLS_CALL,
                "1",
                objectMapper.convertValue(callParams, Map.class)
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.CallToolResult.class);

        McpSchema.CallToolResult callResult = (McpSchema.CallToolResult) result.get();
        assertThat(callResult.isError()).isFalse();

        verify(mockTool).apply(eq(permissionHelper), eq(args));
        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testCallToolWithInvalidToolName() throws Exception {
        // Given
        var callParams = new McpSchema.CallToolRequest("nonexistent_tool", Map.of());
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_TOOLS_CALL,
                "1",
                objectMapper.convertValue(callParams, Map.class)
        );

        // When
        Optional<McpSchema.Result> result = handle(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.CallToolResult.class);

        McpSchema.CallToolResult callResult = (McpSchema.CallToolResult) result.get();
        assertThat(callResult.isError()).isTrue();

        verify(auditEventSender).failure(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testCallToolReturnsCallToolError() throws Exception {
        // Given
        Tool<Map<String, Object>, String> mockTool = mock(Tool.class);
        when(mockTool.apply(eq(permissionHelper), any())).thenThrow(new RuntimeException("Tool error"));

        tools.put("test_tool", mockTool);

        var callParams = new McpSchema.CallToolRequest("test_tool", Map.of());
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_TOOLS_CALL,
                "1",
                objectMapper.convertValue(callParams, Map.class)
        );

        // When/Then
        assertThat(handle(request)).get().satisfies(result -> {
            assertThat(result).isInstanceOf(McpSchema.CallToolResult.class);
            final McpSchema.CallToolResult callResult = (McpSchema.CallToolResult) result;
            assertThat(callResult.isError()).isTrue();
        });

        verify(auditEventSender).failure(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testListPrompts() {
        // Given
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_PROMPT_LIST,
                "1",
                null
        );

        // When/Then
        assertThatNoException().isThrownBy(() -> {
            Optional<McpSchema.Result> result = handle(request);

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(McpSchema.ListPromptsResult.class);

            McpSchema.ListPromptsResult listResult = (McpSchema.ListPromptsResult) result.get();
            assertThat(listResult.prompts()).hasSize(0);
        });

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    @Disabled("not handling prompts for now")
    void testGetPrompt() {
        // Given
        var promptParams = new McpSchema.GetPromptRequest("log_sources_analysis", null);
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_PROMPT_GET,
                "1",
                objectMapper.convertValue(promptParams, Map.class)
        );

        // When/Then
        assertThatNoException().isThrownBy(() -> {
            Optional<McpSchema.Result> result = handle(request);

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(McpSchema.GetPromptResult.class);

            McpSchema.GetPromptResult promptResult = (McpSchema.GetPromptResult) result.get();
            assertThat(promptResult.messages()).hasSize(1);
            assertThat(promptResult.messages().getFirst().role()).isEqualTo(McpSchema.Role.USER);
        });

        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testGetPromptWithInvalidName() {
        // Given
        var promptParams = new McpSchema.GetPromptRequest("invalid_prompt", null);
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                McpSchema.METHOD_PROMPT_GET,
                "1",
                objectMapper.convertValue(promptParams, Map.class)
        );

        // When/Then
        assertThatThrownBy(() -> handle(request))
                .isInstanceOf(McpError.class)
                .hasMessageContaining("Unsupported method");
    }

    @Test
    void testUnsupportedMethod() {
        // Given
        var request = new McpSchema.JSONRPCRequest(
                "2.0",
                "unsupported_method",
                "1",
                null
        );

        // When/Then
        assertThatThrownBy(() -> handle(request))
                .isInstanceOf(McpError.class)
                .hasMessageContaining("Unsupported method");
    }

    @Test
    void testCallToolRejectsInvalidArgumentsWhenValidationEnabled() throws Exception {
        final McpConfiguration config = McpConfiguration.create(false, false, true);

        Tool<Map<String, Object>, String> mockTool = mock(Tool.class);
        when(mockTool.inputSchema()).thenReturn(Map.of(
                "type", "object",
                "properties", Map.of("limit", Map.of("type", "integer")),
                "required", List.of("limit")));
        tools.put("test_tool", mockTool);

        // missing the required "limit" property
        var callParams = new McpSchema.CallToolRequest("test_tool", Map.of());
        var request = new McpSchema.JSONRPCRequest("2.0", McpSchema.METHOD_TOOLS_CALL, "1",
                objectMapper.convertValue(callParams, Map.class));

        Optional<McpSchema.Result> result = mcpService.handle(permissionHelper, request, "session123", null, config);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.CallToolResult.class);
        final McpSchema.CallToolResult callResult = (McpSchema.CallToolResult) result.get();
        assertThat(callResult.isError()).isTrue();
        verify(mockTool, never()).apply(any(), any());
        verify(auditEventSender).failure(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }

    @Test
    void testCallToolAcceptsValidArgumentsWhenValidationEnabled() throws Exception {
        final McpConfiguration config = McpConfiguration.create(false, false, true);

        Tool<Map<String, Object>, String> mockTool = mock(Tool.class);
        when(mockTool.inputSchema()).thenReturn(Map.of(
                "type", "object",
                "properties", Map.of("limit", Map.of("type", "integer")),
                "required", List.of("limit")));
        when(mockTool.outputSchema()).thenReturn(Optional.empty());
        when(mockTool.apply(eq(permissionHelper), any())).thenReturn("Tool result");
        tools.put("test_tool", mockTool);

        var callParams = new McpSchema.CallToolRequest("test_tool", Map.of("limit", 5));
        var request = new McpSchema.JSONRPCRequest("2.0", McpSchema.METHOD_TOOLS_CALL, "1",
                objectMapper.convertValue(callParams, Map.class));

        Optional<McpSchema.Result> result = mcpService.handle(permissionHelper, request, "session123", null, config);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(McpSchema.CallToolResult.class);
        final McpSchema.CallToolResult callResult = (McpSchema.CallToolResult) result.get();
        assertThat(callResult.isError()).isFalse();
        verify(mockTool).apply(eq(permissionHelper), any());
        verify(auditEventSender).success(any(AuditActor.class), any(AuditEventType.class), anyMap());
    }
}
