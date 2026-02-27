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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventType;
import org.graylog2.shared.ServerVersion;
import org.graylog2.web.customization.CustomizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.audit.AuditEventTypes.MCP_PROMPT_GET;
import static org.graylog2.audit.AuditEventTypes.MCP_PROMPT_LIST;
import static org.graylog2.audit.AuditEventTypes.MCP_PROTOCOL_INITIALIZE;
import static org.graylog2.audit.AuditEventTypes.MCP_RESOURCE_LIST;
import static org.graylog2.audit.AuditEventTypes.MCP_RESOURCE_READ;
import static org.graylog2.audit.AuditEventTypes.MCP_RESOURCE_READTEMPLATES;
import static org.graylog2.audit.AuditEventTypes.MCP_TOOL_CALL;
import static org.graylog2.audit.AuditEventTypes.MCP_TOOL_LIST;
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class McpService {
    private static final Logger LOG = LoggerFactory.getLogger(McpService.class);
    private static final String LATEST_SUPPORTED_MCP_VERSION = ProtocolVersions.MCP_2025_06_18;
    static final String FALLBACK_MCP_VERSION = "2025-03-26";
    static final List<String> ALL_SUPPORTED_MCP_VERSIONS = List.of(LATEST_SUPPORTED_MCP_VERSION);

    private final ObjectMapper objectMapper;
    private final AuditEventSender auditEventSender;
    private final CustomizationConfig customizationConfig;
    private final GRNRegistry grnRegistry;
    private final Map<String, Tool<?, ?>> tools;
    private final Map<GRNType, ? extends ResourceProvider> resourceProviders;

    @Inject
    protected McpService(ObjectMapper objectMapper,
                         AuditEventSender auditEventSender,
                         CustomizationConfig customizationConfig,
                         GRNRegistry grnRegistry,
                         Map<String, Tool<?, ?>> tools,
                         Map<GRNType, ? extends ResourceProvider> resourceProviders) {
        this.objectMapper = objectMapper;
        this.auditEventSender = auditEventSender;
        this.customizationConfig = customizationConfig;
        this.grnRegistry = grnRegistry;
        this.tools = tools;
        this.resourceProviders = resourceProviders;
    }

    public Optional<McpSchema.Result> handle(PermissionHelper permissionHelper, McpSchema.JSONRPCRequest request, String sessionId) throws McpError {
        return handle(permissionHelper, request, sessionId, null);
    }

    public Optional<McpSchema.Result> handle(PermissionHelper permissionHelper, McpSchema.JSONRPCRequest request, String sessionId, String protocolVersion) throws McpError {
        final AuditActor auditActor = AuditActor.user(permissionHelper.getCurrentUser().getName());
        final Map<String, Object> auditContext = Maps.newHashMap();
        auditContext.put("sessionId", sessionId);

        // TODO: support multiple protocol versions -> add different handlers for different protocol versions if required

        switch (request.method()) {
            case McpSchema.METHOD_INITIALIZE -> {
                final McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                auditContext.put("request", initializeRequest);
                final McpSchema.InitializeResult result = new McpSchema.InitializeResult(
                        // Version negotiation: https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle#version-negotiation
                        ALL_SUPPORTED_MCP_VERSIONS.contains(initializeRequest.protocolVersion()) ? initializeRequest.protocolVersion() : LATEST_SUPPORTED_MCP_VERSION,
                        new McpSchema.ServerCapabilities(
                                null,
                                null,
                                null,
                                new McpSchema.ServerCapabilities.PromptCapabilities(false),
                                new McpSchema.ServerCapabilities.ResourceCapabilities(false, false),
                                new McpSchema.ServerCapabilities.ToolCapabilities(false)
                        ),
                        new McpSchema.Implementation(customizationConfig.productName(), ServerVersion.VERSION.toString()),
                        null,
                        null);
                auditEventSender.success(auditActor, AuditEventType.create(MCP_PROTOCOL_INITIALIZE), auditContext);
                return Optional.of(result);
            }
            case McpSchema.METHOD_PING -> {
                return Optional.empty();
            }
            case McpSchema.METHOD_RESOURCES_LIST -> {
                LOG.debug("Listing available resources");
                // TODO pagination needs to hold a cursor across _all_ resource types, which we don't have support for
                // currently, so we need to skip it at the moment. MCP doesn't have any way to scope it to resource types
                // so we are a bit dead in the water in the way we need to adapt it.
                final List<McpSchema.Resource> resourceList = this.resourceProviders.values().stream()
                        .map(resourceProvider -> resourceProvider.list(permissionHelper))
                        .flatMap(List::stream)
                        .toList();
                final McpSchema.ListResourcesResult result = new McpSchema.ListResourcesResult(resourceList, null);
                LOG.debug("Returning available resources {}", result);
                auditEventSender.success(auditActor, AuditEventType.create(MCP_RESOURCE_LIST), auditContext);
                return Optional.of(result);
            }
            case McpSchema.METHOD_RESOURCES_READ -> {
                final McpSchema.ReadResourceRequest readResourceRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                auditContext.put("request", readResourceRequest);
                LOG.debug("Reading resource: {}", readResourceRequest);
                try {
                    final McpSchema.Resource resource = this.resourceProviders
                            .get(grnRegistry.parse(readResourceRequest.uri()).grnType())
                            .read(permissionHelper, new URI(readResourceRequest.uri()))
                            .orElseThrow();
                    auditEventSender.success(auditActor, AuditEventType.create(MCP_RESOURCE_READ), auditContext);
                    final var contents = new McpSchema.TextResourceContents(resource.uri(), null, resource.description());
                    return Optional.of(new McpSchema.ReadResourceResult(List.of(contents)));
                } catch (Exception e) {
                    throw McpError.builder(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND)
                            .message("Failed to read resource")
                            .data(Map.of("uri", readResourceRequest.uri()))
                            .build();
                }
            }
            case McpSchema.METHOD_RESOURCES_TEMPLATES_LIST -> {
                LOG.debug("Listing available resource templates");
                final List<McpSchema.ResourceTemplate> templates = resourceProviders.values().stream()
                        .map(ResourceProvider::resourceTemplate)
                        .map(template -> new McpSchema.ResourceTemplate(
                                template.uriTemplate().getTemplate(),
                                template.name(),
                                template.title(),
                                template.description(),
                                template.contentType(),
                                null
                        ))
                        .toList();
                auditEventSender.success(auditActor, AuditEventType.create(MCP_RESOURCE_READTEMPLATES), auditContext);
                return Optional.of(new McpSchema.ListResourceTemplatesResult(templates, null));
            }
            case McpSchema.METHOD_TOOLS_LIST -> {
                LOG.debug("Listing available tools");
                final List<McpSchema.Tool> toolList = this.tools.values().stream().map(tool -> {
                    var builder = McpSchema.Tool.builder()
                            .name(tool.name())
                            .title(tool.title())
                            .description(tool.description());
                    tool.inputSchema().ifPresent(builder::inputSchema);
                    if (tool.isOutputSchemaEnabled()) {
                        tool.outputSchema().ifPresent(builder::outputSchema);
                    }
                    return builder.build();
                }).toList();
                auditEventSender.success(auditActor, AuditEventType.create(MCP_TOOL_LIST), auditContext);
                return Optional.of(new McpSchema.ListToolsResult(toolList, null));
            }
            case McpSchema.METHOD_TOOLS_CALL -> {
                final McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                auditContext.put("request", callToolRequest);

                LOG.debug("Calling MCP tool: {}", callToolRequest);
                if (tools.containsKey(callToolRequest.name())) {
                    final Tool<?, ?> tool = tools.get(callToolRequest.name());
                    try {
                        final Object result = tool.apply(permissionHelper, callToolRequest.arguments());
                        if (tool.outputSchema().isPresent()) {
                            // if we have an output schema we want to return structured content
                            try {
                                var structuredContent = objectMapper.convertValue(result,
                                        new TypeReference<Map<String, Object>>() {
                                        });
                                auditEventSender.success(auditActor, AuditEventType.create(MCP_TOOL_CALL),
                                        auditContext);
                                return Optional.of(new McpSchema.CallToolResult(
                                        List.of(new McpSchema.TextContent(objectMapper.writeValueAsString(result))),
                                        false,
                                        structuredContent));
                            } catch (JsonProcessingException e) {
                                auditEventSender.failure(auditActor, AuditEventType.create(MCP_TOOL_CALL),
                                        auditContext);
                                throw new RuntimeException(e);
                            }
                        } else {
                            // no schema, just return the string representation directly
                            auditEventSender.success(auditActor, AuditEventType.create(MCP_TOOL_CALL), auditContext);
                            return Optional.of(new McpSchema.CallToolResult(result.toString(), false));
                        }
                    } catch (Exception e) {
                        auditEventSender.failure(auditActor, AuditEventType.create(MCP_TOOL_CALL), auditContext);
                        return Optional.of(new McpSchema.CallToolResult(f("Tool call failed: %s", e.getMessage()), true));
                    }
                } else {
                    auditEventSender.failure(auditActor, AuditEventType.create(MCP_TOOL_CALL), auditContext);
                    return Optional.of(new McpSchema.CallToolResult("Unknown tool named: " + callToolRequest.name(), true));
                }
            }
            case McpSchema.METHOD_PROMPT_LIST -> {
                LOG.debug("Listing available prompts");
                auditEventSender.success(auditActor, AuditEventType.create(MCP_PROMPT_LIST), auditContext);
                return Optional.of(new McpSchema.ListPromptsResult(List.of(), null));
            }
            case McpSchema.METHOD_PROMPT_GET -> {
                // disabled for now
                final McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                auditContext.put("request", promptRequest);
                LOG.debug("Getting prompt {}", promptRequest.name());
                auditEventSender.failure(auditActor, AuditEventType.create(MCP_PROMPT_GET), auditContext);
//                return Optional.of(new McpSchema.GetPromptResult(null, List.of()));
            }
            default -> LOG.warn("Unsupported MCP method: {}", request.method());

        }
        throw McpError.builder(McpSchema.ErrorCodes.METHOD_NOT_FOUND)
                .message("Unsupported method: " + request.method())
                .build();
    }
}
