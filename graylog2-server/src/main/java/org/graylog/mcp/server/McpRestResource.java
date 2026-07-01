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
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.SkipCSRFProtection;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@RequiresAuthentication
@Path("/mcp")
@PublicCloudAPI
@Tag(name = "MCP", description = "Endpoints allowing MCP clients to connect to")
public class McpRestResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(McpRestResource.class);
    private static final String HEADER_MCP_SESSION_ID = "Mcp-Session-Id";
    private static final String HEADER_MCP_PROTOCOL_VERSION = "MCP-Protocol-Version";

    private final McpService mcpService;

    private final SecurityContext securityContext;

    private final ClusterConfigService clusterConfig;

    @Inject
    public McpRestResource(final ClusterConfigService clusterConfig, final McpService mcpService, final SecurityContext securityContext) {
        this.clusterConfig = clusterConfig;
        this.mcpService = mcpService;
        this.securityContext = securityContext;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("server-to-server")
    @RequiresPermissions(RestPermissions.MCP_SERVER_ACCESS)
    @NoAuditEvent("Has custom audit events")
    @Operation(summary = "JSON-RPC endpoint for MCP clients to connect to")
    public Response post(@HeaderParam(HttpHeaders.ACCEPT) String acceptHeader,
                         @HeaderParam(HEADER_MCP_PROTOCOL_VERSION) String protocolVersionHeader,
                         @HeaderParam(HEADER_MCP_SESSION_ID) String mcpSessionIdHeader,
                         @Context SearchUser searchUser,
                         @Parameter(name = "jsonrpc_message", required = true) JsonNode payload) {
        final McpConfiguration mcpConfig = clusterConfig.getOrDefault(McpConfiguration.class,
                McpConfiguration.DEFAULT_VALUES);
        if (!mcpConfig.enableRemoteAccess()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            // nothing to handle
            return Response.accepted().build();
        }

        // TODO: manage session ids -> store session ids
        final String sessionId = Optional.ofNullable(mcpSessionIdHeader)
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        // We only implement non-streaming for now if the client is willing to go one-shot. we might support SSE later.
        final String accept = Optional.ofNullable(acceptHeader).orElse("");
        final boolean stream = accept.contains(MediaType.SERVER_SENT_EVENTS) && !accept.contains(MediaType.APPLICATION_JSON);

        final McpSchema.JSONRPCMessage message;
        try {
            message = mcpService.parseMessage(payload);
        } catch (Exception e) {
            // Not a recognisable JSON-RPC message; we have no id to respond against, so reject the request.
            LOG.warn("Received an unparseable JSON-RPC message", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!(message instanceof McpSchema.JSONRPCRequest request)) {
            // responses and notifications must not create new responses
            // (https://modelcontextprotocol.io/specification/2025-06-18/basic#notifications)
            return Response.accepted().build();
        }
        LOG.trace("Received JSON-RPC request {}", request);

        if (stream) {
            // Simple one-shot JSON reply only; SSE streaming is not implemented yet.
            LOG.warn("SSE is not supported at the moment, returning 405 Method Not Allowed");
            return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
        }

        final Object id = request.id();
        try {
            if (protocolVersionHeader != null) {
                // According to: https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#protocol-version-header
                // The protocol version sent by the client SHOULD be the one negotiated during initialization.
                // The server is NOT responsible for ensuring that the negotiated version remains the same over multiple requests.
                if (!McpService.ALL_SUPPORTED_MCP_VERSIONS.contains(protocolVersionHeader)) {
                    LOG.warn("Invalid protocol version for request header {}", protocolVersionHeader);
                    // Example: https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle#error-handling
                    throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                            .message("Invalid protocol version header " + protocolVersionHeader)
                            .data(Map.of("supported", McpService.ALL_SUPPORTED_MCP_VERSIONS, "requested", protocolVersionHeader))
                            .build();
                }
            } else {
                // TODO: manage session ids -> retrieve negotiated version as fallback, use "2025-03-26" as default
                protocolVersionHeader = McpService.FALLBACK_MCP_VERSION;
            }
            final PermissionHelper permissionHelper = new PermissionHelper(getCurrentUser(), securityContext, searchUser);
            final Optional<McpSchema.Result> result = mcpService.handle(permissionHelper, request, sessionId, protocolVersionHeader, mcpConfig);

            if (result.isPresent() && LOG.isTraceEnabled()) {
                LOG.trace("Successfully handled JSON-RPC request: {}", result.get());
            }
            return Response.ok(new McpSchema.JSONRPCResponse("2.0", id, result.orElse(null), null))
                    .header(HEADER_MCP_SESSION_ID, sessionId)
                    .build();
        } catch (McpError e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new McpSchema.JSONRPCResponse("2.0", id, null, e.getJsonRpcError()))
                    .header(HEADER_MCP_SESSION_ID, sessionId)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new McpSchema.JSONRPCResponse("2.0",
                            id,
                            null,
                            new McpSchema.JSONRPCResponse.JSONRPCError(
                                    McpSchema.ErrorCodes.INTERNAL_ERROR,
                                    e.getMessage(),
                                    null
                            )
                    ))
                    .header(HEADER_MCP_SESSION_ID, sessionId)
                    .build();
        }
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SkipCSRFProtection("server-to-server")
    @RequiresPermissions(RestPermissions.MCP_SERVER_ACCESS)
    @NoAuditEvent("prototype")
    @Operation(summary = "Unused endpoint for MCP protocol compatibility")
    public Response get() {
        final McpConfiguration mcpConfig = clusterConfig.getOrDefault(McpConfiguration.class,
                McpConfiguration.DEFAULT_VALUES);
        if (!mcpConfig.enableRemoteAccess()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }

    // TODO: manage session ids -> DELETE session ids and return NO_CONTENT

}
