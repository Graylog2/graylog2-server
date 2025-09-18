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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.shared.rest.SkipCSRFProtection;
import org.graylog2.shared.rest.resources.RestResource;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Path("/mcp")
public class McpResource extends RestResource {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    McpService mcpService;

    @Inject
    SecurityContext securityContext;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("server-to-server")
    @NoAuditEvent("prototype")
    public Response post(@Context HttpHeaders headers, String body) throws IOException {
        final String accept = Optional.ofNullable(headers.getHeaderString(HttpHeaders.ACCEPT)).orElse("");
        final JsonNode payload = (body == null || body.isEmpty()) ? NullNode.getInstance() : objectMapper.readTree(body);

        if (!containsAnyRequests(payload)) {
            // no requests, simply respond, responses or notifications
            // must not create new responses (https://modelcontextprotocol.io/specification/2025-06-18/basic#notifications)
            return Response.accepted().build();
        }

        // TODO we don't actually use the session id right now, figure out whether it needs to go into the DB for progress notifications
        final String sessionId = Optional.ofNullable(headers.getHeaderString("Mcp-Session-Id"))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        // According to: https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#protocol-version-header
        // TODO: (mcpService.currentSessions.get(sessionId).negotiatedProtocol() != headers.getHeaderString("MCP-Protocol-Version"))
        if (Optional.ofNullable(headers.getHeaderString("MCP-Protocol-Version"))
                .map(mcpService.supportedVersions::contains)
                .orElse(false)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(objectMapper.createObjectNode().put("error", "Invalid protocol version"))
                    .build();
        }

        // TODO: prefers non-streaming for now if the client is willing to go one-shot. we should still support SSE eventually.
        final boolean stream = accept.contains("text/event-stream") && !accept.contains("application/json");
        if (!stream) {
            // Simple one-shot JSON reply
            final McpSchema.JSONRPCRequest request = objectMapper.convertValue(payload, McpSchema.JSONRPCRequest.class);
            try {
                final Optional<McpSchema.Result> result = mcpService.handle(securityContext, request, sessionId);

                return Response.ok(new McpSchema.JSONRPCResponse("2.0",
                                request.id(),
                                result.orElse(null),
                                null))
                        .header("Mcp-Session-Id", sessionId)
                        .build();
            } catch (IllegalArgumentException e) {
                JsonNode data = objectMapper.nullNode();
                if (request.method().equals(McpSchema.METHOD_INITIALIZE)) {
                    // Example: https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle#error-handling
                    data = objectMapper.createObjectNode().set("supported", objectMapper.valueToTree(mcpService.supportedVersions));
                }
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new McpSchema.JSONRPCResponse("2.0",
                                request.id(),
                                null,
                                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, e.getMessage(), data)))
                        .header("Mcp-Session-Id", sessionId)
                        .build();
            } catch (McpException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new McpSchema.JSONRPCResponse("2.0",
                                request.id(),
                                null,
                                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, e.getMessage(), null)))
                        .header("Mcp-Session-Id", sessionId)
                        .build();
            }

        }

        // TODO we should support SSE as well
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }

    @GET
    @Produces("text/event-stream")
    @SkipCSRFProtection("server-to-server")
    @NoAuditEvent("prototype")
    public Response get() {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }

    private boolean containsAnyRequests(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return false;
        if (node.isObject()) {
            return node.hasNonNull("method") && node.has("id");
        }
        if (node.isArray()) {
            for (JsonNode n : node) {
                if (containsAnyRequests(n)) return true;
            }
        }
        return false;
    }

    private String toJson(Object o) {
        try {
            if (o instanceof String s) return s;
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"" + e.getMessage() + "\"}}";
        }
    }
}
