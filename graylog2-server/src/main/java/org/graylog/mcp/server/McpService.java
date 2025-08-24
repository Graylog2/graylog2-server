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

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.ServerVersion;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

@Singleton
public class McpService {
    private static final Logger LOG = LoggerFactory.getLogger(McpService.class);

    private final ObjectMapper objectMapper;
    private final StreamService streamService;

    @Inject
    protected McpService(ObjectMapper objectMapper, StreamService streamService) {
        this.objectMapper = objectMapper;
        this.streamService = streamService;
    }

    public Optional<McpSchema.Result> handle(McpSchema.JSONRPCRequest request, String sessionId) throws McpException {

        switch (request.method()) {
            case McpSchema.METHOD_INITIALIZE -> {
                final McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                if (!ProtocolVersions.MCP_2025_06_18.equals(initializeRequest.protocolVersion())) {
                    LOG.warn("Invalid protocol version {} for request {}", initializeRequest.protocolVersion(), request.params());
                    throw new IllegalArgumentException("Invalid protocol version " + initializeRequest.protocolVersion());
                }
                return Optional.of(new McpSchema.InitializeResult(ProtocolVersions.MCP_2025_06_18,
                        new McpSchema.ServerCapabilities(
                                null,
                                null,
                                null,
                                null,
                                null,
                                new McpSchema.ServerCapabilities.ToolCapabilities(true)
                        ),
                        new McpSchema.Implementation("Graylog", ServerVersion.VERSION.toString()),
                        null,
                        null));
            }
            case McpSchema.METHOD_PING -> {
                return Optional.empty();
            }
            case McpSchema.METHOD_TOOLS_LIST -> {
                LOG.info("Listing available tools");
                List<McpSchema.Tool> tools = Lists.newArrayList();
                tools.add(McpSchema.Tool.builder()
                        .name("list_streams")
                        .description("List of streams available in this Graylog cluster in CSV format")
                        .inputSchema("""
                                {
                                   "type": "object",
                                   "properties": {},
                                   "required": []
                                 }
                                """)
                        .build());
                return Optional.of(new McpSchema.ListToolsResult(tools, null));
            }
            case McpSchema.METHOD_TOOLS_CALL -> {
                final McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(request.params(), new TypeReference<>() {});
                LOG.info("Calling MCP tool: {}", callToolRequest);
                switch (callToolRequest.name()) {
                    case "list_streams" -> {
                        final List<Stream> streams = streamService.loadAll();
                        final StringWriter writer = new StringWriter();
                        final CSVWriter csvWriter = new CSVWriter(writer);
                        csvWriter.writeNext(new String[]{"id", "name", "description", "index_set"});
                        streams.forEach(stream -> {
                            csvWriter.writeNext(new String[]{stream.getId(), stream.getTitle(), stream.getDescription(), stream.getIndexSet().getConfig().title()});
                        });
                        return Optional.of(McpSchema.CallToolResult.builder()
                                .addTextContent(writer.toString())
                                .build());
                    }
                    default -> throw new McpException("Unknown tool named: " + callToolRequest.name());
                }
            }
            default -> LOG.warn("Unsupported MCP method: " + request.method());

        }
        throw new McpException("Unsupported request method: " + request.method());
    }
}
