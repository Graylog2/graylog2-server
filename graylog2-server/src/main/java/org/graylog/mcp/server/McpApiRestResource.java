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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Path("/mcp/api")
@Api(value = "MCP API", description = "Endpoints to retrieve current MCP status", tags = {CLOUD_VISIBLE})
public class McpApiRestResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(McpApiRestResource.class);

    private final McpService mcpService;

    private final SecurityContext securityContext;

    private final ClusterConfigService clusterConfig;

    @Inject
    public McpApiRestResource(final ClusterConfigService clusterConfig, final McpService mcpService, final SecurityContext securityContext) {
        this.clusterConfig = clusterConfig;
        this.mcpService = mcpService;
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

        var payload = Map.of(
                "remote_access_enabled",  mcpConfig.enableRemoteAccess()
        );

        return Response.ok().entity(payload).build();
    }

    @GET
    @Path("/tools")
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("server-to-server")
    @RequiresPermissions(RestPermissions.MCP_SERVER_ACCESS)
    @NoAuditEvent("prototype")
    @ApiOperation("List available tools")
    public Response getTools() {
        var tools = mcpService.getTools().values().stream().map(tool -> Map.of(
                "name", tool.name(),
                "enabled", tool.isEnabled(),
                "output_format", tool.getOutputFormat().toString(),
                "format_overridden", tool.isOutputFormatOverridden(),
                "category", tool.getCategory(),
                "read_only", tool.hasReadOnlyBehavior()
        )).toList();

        return Response.ok().entity(Map.of("tools", tools)).build();
    }

    @POST
    @Path("/tools")
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("server-to-server")
    @RequiresPermissions(RestPermissions.MCP_SERVER_ACCESS)
    @NoAuditEvent("prototype")
    @ApiOperation("Updates multiple tools")
    public Response updateTools(@ApiParam(value = "tools", required = true) List<ToolPatch> payload) {
        if (!getMcpConfig().enableRemoteAccess()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }

        List<String> errors = new ArrayList<>();
        for (ToolPatch receivedPatch : payload) {
            try {
                applyPatch(receivedPatch);
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
            }
        }

        return (errors.isEmpty() ? Response.ok() : Response.status(Response.Status.INTERNAL_SERVER_ERROR))
                .entity(Map.of("errors", errors)).build();
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

    private McpConfiguration getMcpConfig() {
        return clusterConfig.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES);
    }

    public record ToolPatch(@JsonProperty("name") String name,
                            @JsonProperty("enabled") Boolean enabled,
                            @JsonProperty("output_format") Tool.OutputFormat outputFormat
    ) {}
}
