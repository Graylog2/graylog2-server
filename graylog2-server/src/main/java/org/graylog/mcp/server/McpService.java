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
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.SecurityContext;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.shared.ServerVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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
                                new McpSchema.ServerCapabilities.PromptCapabilities(false),
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
                LOG.info("Listing available resources");
                // TODO pagination needs to hold a cursor across _all_ resource types, which we don't have support for
                // currently, so we need to skip it at the moment. MCP doesn't have any way to scope it to resource types
                // so we are a bit dead in the water in the way we need to adapt it.
                final List<McpSchema.Resource> resourceList = this.resourceProviders.values().stream()
                        .map(resourceProvider -> resourceProvider.list(null, null))
                        .flatMap(List::stream)
                        .toList();
                final McpSchema.ListResourcesResult result = new McpSchema.ListResourcesResult(resourceList, null);
                LOG.info("Returning available resources {}", result);
                return Optional.of(result);
            }
            case McpSchema.METHOD_RESOURCES_READ -> {
                final McpSchema.ReadResourceRequest readResourceRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                McpSchema.ResourceContents contents;
                try {
                    final McpSchema.Resource resource = this.resourceProviders
                            .get(GRNRegistry.createWithBuiltinTypes().parse(readResourceRequest.uri()).grnType())
                            .read(new URI(readResourceRequest.uri())
                            );
                    contents = new McpSchema.TextResourceContents(resource.uri(), null, resource.description());
                } catch (Exception e) {
                    throw new McpException("Failed to read resource: " + e.getMessage());
                }
                return Optional.of(new McpSchema.ReadResourceResult(List.of(contents)));
            }
            case McpSchema.METHOD_RESOURCES_TEMPLATES_LIST -> {
                LOG.info("Listing available resource templates");
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

                return Optional.of(new McpSchema.ListResourceTemplatesResult(templates, null));
            }
            case McpSchema.METHOD_TOOLS_LIST -> {
                LOG.info("Listing available tools");
                final List<McpSchema.Tool> toolList = this.tools.values().stream().map(tool -> {
                    var builder = McpSchema.Tool.builder()
                            .name(tool.name())
                            .title(tool.title())
                            .description(tool.description())
                            .inputSchema(tool.inputSchema());
                    if (tool.outputSchema().isPresent()) {
                        builder.outputSchema(tool.outputSchema().get());
                    }
                    return builder.build();
                }).toList();

                return Optional.of(new McpSchema.ListToolsResult(toolList, null));
            }
            case McpSchema.METHOD_TOOLS_CALL -> {
                final McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                LOG.info("Calling MCP tool: {}", callToolRequest);
                if (tools.containsKey(callToolRequest.name())) {
                    final Tool<?, ?> tool = tools.get(callToolRequest.name());
                    final Object result = tool.apply(new PermissionHelper(securityContext), callToolRequest.arguments());
                    if (tool.outputSchema().isPresent()) {
                        // if we have an output schema we want to return structured content
                        try {
                            var structuredContent = objectMapper.convertValue(result,
                                                                              new TypeReference<Map<String, Object>>() {
                                                                              });
                            return Optional.of(new McpSchema.CallToolResult(
                                    List.of(new McpSchema.TextContent(objectMapper.writeValueAsString(result))), false,
                                    structuredContent));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        // no schema, just return the string representation directly
                        return Optional.of(new McpSchema.CallToolResult(result.toString(), false));
                    }
                } else {
                    throw new McpException("Unknown tool named: " + callToolRequest.name());
                }
            }
            case McpSchema.METHOD_PROMPT_LIST -> {
                LOG.info("Listing available prompts");
                return Optional.of(new McpSchema.ListPromptsResult(List.of(
                        new McpSchema.Prompt(
                                "log_sources_analysis",
                                "Analyze current log source state",
                                """
                                        Asks the LLM to look at the log sources currently ingested over a certain
                                        period of time, and how those have changed in characteristics, in order to
                                        understand if something important has changed or needs attention
                                        """,
                                null
                        )

                ), null));
            }
            case McpSchema.METHOD_PROMPT_GET -> {
                final McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                LOG.info("Getting prompt {}", promptRequest.name());
                var result = switch (promptRequest.name()) {
                    case "log_sources_analysis" -> new McpSchema.PromptMessage(
                            McpSchema.Role.ASSISTANT,
                            new McpSchema.TextContent("""
                                    You are an administrator for the log management system Graylog and are tasked with
                                     understanding how the log sources sending data into Graylog are behaving over the
                                     last two days. Use the current day and compare the situation of the log sources to
                                     the previous 24 hours, with special attention to sources that have stopped sending
                                     or are now sending extraordinary amounts of data.""")
                    );
                    default -> throw new McpException("Unknown prompt name: " + promptRequest.name());
                };
                return Optional.of(new McpSchema.GetPromptResult("", List.of(result)));
            }
            default -> LOG.warn("Unsupported MCP method: " + request.method());

        }
        throw new McpException("Unsupported request method: " + request.method());
    }
}
