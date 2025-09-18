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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.SecurityContext;
import org.graylog.grn.GRNType;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.shared.ServerVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class McpService {
    private static final Logger LOG = LoggerFactory.getLogger(McpService.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Tool<?, ?>> tools;
    private final Map<GRNType, ? extends ResourceProvider> resourceProviders;
    protected final List<String> supportedVersions = List.of(ProtocolVersions.MCP_2025_06_18);

    @Inject
    protected McpService(ObjectMapper objectMapper,
                         Map<String, Tool<?, ?>> tools,
                         Map<GRNType, ? extends ResourceProvider> resourceProviders) {
        this.objectMapper = objectMapper;
        this.tools = tools;
        this.resourceProviders = resourceProviders;
    }

    public Optional<McpSchema.Result> handle(SecurityContext securityContext, McpSchema.JSONRPCRequest request, String sessionId) throws McpException, IllegalArgumentException {

        switch (request.method()) {
            case McpSchema.METHOD_INITIALIZE -> {
                final McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                if (!supportedVersions.contains(initializeRequest.protocolVersion())) {
                    LOG.warn("Invalid protocol version {} for request {}", initializeRequest.protocolVersion(), request.params());
                    throw new IllegalArgumentException("Invalid protocol version " + initializeRequest.protocolVersion());
                }
                return Optional.of(new McpSchema.InitializeResult(initializeRequest.protocolVersion(),
                        new McpSchema.ServerCapabilities(
                                null,
                                null,
                                null,
                                null,
                                new McpSchema.ServerCapabilities.ResourceCapabilities(false, false),
                                new McpSchema.ServerCapabilities.ToolCapabilities(false)
                        ),
                        new McpSchema.Implementation("Graylog", ServerVersion.VERSION.toString()),
                        null,
                        null));
            }
            case McpSchema.METHOD_PING -> {
                return Optional.empty();
            }
            case McpSchema.METHOD_RESOURCES_LIST -> {
                // TODO pagination needs to hold a cursor across _all_ resource types, which we don't have support for
                // currently, so we need to skip it at the moment. MCP doesn't have any way to scope it to resource types
                // so we are a bit dead in the water in the way we need to adapt it.
                final List<McpSchema.Resource> resourceList = this.resourceProviders.values().stream()
                        .map(resourceProvider -> resourceProvider.list(null))
                        .flatMap(List::stream)
                        .toList();
                return Optional.of(new McpSchema.ListResourcesResult(resourceList, null));
            }
            case McpSchema.METHOD_RESOURCES_READ -> {
                final McpSchema.ReadResourceRequest readResourceRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                return Optional.of(new McpSchema.ReadResourceResult(List.of()));
            }
            case McpSchema.METHOD_TOOLS_LIST -> {
                LOG.info("Listing available tools");
                final List<McpSchema.Tool> toolList = this.tools.values().stream().map(tool -> McpSchema.Tool.builder()
                        .name(tool.name())
                        .title(tool.title())
                        .description(tool.description())
                        .inputSchema(tool.inputSchema())
                        .build()).toList();
                return Optional.of(new McpSchema.ListToolsResult(toolList, null));
            }
            case McpSchema.METHOD_TOOLS_CALL -> {
                final McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                LOG.info("Calling MCP tool: {}", callToolRequest);
                if (tools.containsKey(callToolRequest.name())) {
                    final Tool<?, ?> tool = tools.get(callToolRequest.name());
                    final Object result = tool.apply(new PermissionHelper(securityContext), callToolRequest.arguments());
                    return Optional.of(new McpSchema.CallToolResult(result.toString(), false));
                } else {
                    throw new McpException("Unknown tool named: " + callToolRequest.name());
                }
            }
            default -> LOG.warn("Unsupported MCP method: " + request.method());

        }
        throw new McpException("Unsupported request method: " + request.method());
    }
}
