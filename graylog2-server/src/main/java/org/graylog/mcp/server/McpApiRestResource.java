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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.SkipCSRFProtection;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Path("/mcp/api")
@Api(value = "MCP API", description = "Endpoints to retrieve current MCP status", tags = {CLOUD_VISIBLE})
public class McpApiRestResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(McpApiRestResource.class);

    private final ObjectMapper objectMapper;

    private final McpService mcpService;

    private final SecurityContext securityContext;

    private final ClusterConfigService clusterConfig;

    @Inject
    public McpApiRestResource(final ClusterConfigService clusterConfig, final McpService mcpService, final ObjectMapper objectMapper, final SecurityContext securityContext) {
        this.clusterConfig = clusterConfig;
        this.mcpService = mcpService;
        this.objectMapper = objectMapper;
        this.securityContext = securityContext;
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("server-to-server")
    @RequiresPermissions(RestPermissions.MCP_SERVER_ACCESS)
    @NoAuditEvent("prototype")
    @ApiOperation("Current status of the MCP server, including available tools and connected clients")
    public Response get() {
        final McpConfiguration mcpConfig = clusterConfig.getOrDefault(McpConfiguration.class,
                McpConfiguration.DEFAULT_VALUES);

        JsonNode payload = objectMapper.createObjectNode()
                .put("remote_access_enabled",  mcpConfig.enableRemoteAccess());

        return Response.ok(payload.toString()).build();
    }

    @GET
    @Path("/tools")
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("server-to-server")
    @RequiresPermissions(RestPermissions.MCP_SERVER_ACCESS)
    @NoAuditEvent("prototype")
    @ApiOperation("List available tools")
    public Response getTools() {
        final McpConfiguration mcpConfig = clusterConfig.getOrDefault(McpConfiguration.class,
                McpConfiguration.DEFAULT_VALUES);
        if (!mcpConfig.enableRemoteAccess()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ArrayNode tools = objectMapper.createArrayNode();
        mcpService.getTools().forEach((name, tool) -> tools.add(jsonTool(tool)));

        JsonNode payload = objectMapper.createObjectNode().set("tools", tools);

        return Response.ok(payload.toString()).build();
    }

    @POST
    @Path("/tools")
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("server-to-server")
    @RequiresPermissions(RestPermissions.MCP_SERVER_ACCESS)
    @NoAuditEvent("prototype")
    @ApiOperation("Updates multiple tools")
    public Response updateTools(@ApiParam(value = "tools", required = true) List<ToolPatch> payload) {
        final McpConfiguration mcpConfig = clusterConfig.getOrDefault(McpConfiguration.class,
                McpConfiguration.DEFAULT_VALUES);
        if (!mcpConfig.enableRemoteAccess()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ArrayNode errors = objectMapper.createArrayNode();
        for (ToolPatch receivedPatch : payload) {
            try {
                applyPatch(receivedPatch);
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
            }
        }

        return (errors.isEmpty() ? Response.ok() : Response.status(Response.Status.INTERNAL_SERVER_ERROR))
                .entity(objectMapper.createObjectNode().set("errors", errors)).build();
    }

    private void applyPatch(ToolPatch payload) {
        if (!mcpService.getTools().containsKey(payload.name)) {
            throw new RuntimeException("Tool " + payload.name + " not found");
        }

        Tool<?, ?> tool = mcpService.getTools().get(payload.name);

        if (payload.enabled != null) {
            tool.setEnabled(payload.enabled);
        }

        if (payload.outputFormat != null) {
            tool.setOutputFormatOverride(payload.outputFormat);
        }
    }

    public record ToolPatch(@JsonProperty("name") String name,
                            @JsonProperty("enabled") Boolean enabled,
                            @JsonProperty("output_format") Tool.OutputFormat outputFormat
    ) {}

    private JsonNode jsonTool(Tool<?, ?> tool) {
        return objectMapper.createObjectNode()
                .put("name", tool.name())
                .put("enabled", tool.isEnabled())
                .put("output_format", tool.getOutputFormat().toString())
                .put("format_overridden", tool.isOutputFormatOverridden())
                .put("category", tool.getCategory())
                .put("read_only",  tool.isReadOnly());
    }
}
