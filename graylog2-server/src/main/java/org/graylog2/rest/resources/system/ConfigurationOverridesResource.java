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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.configuration.overrides.ConfigurationOverrideValue;
import org.graylog2.configuration.overrides.ConfigurationOverridesService;
import org.graylog2.configuration.overrides.NodeType;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Strings.emptyToNull;

/**
 * Manages configuration overrides, which take precedence over the values from the configuration file, environment
 * variables and system properties of a node.
 * <p>
 * Overrides apply either to all nodes of a type or, if a node ID is given, to a single node. Values are stored as raw
 * strings, as they would appear in the configuration file. They aren't validated yet, and neither the parameter names
 * nor the node IDs are checked.
 */
@RequiresAuthentication
@Tag(name = "System/ConfigurationOverrides", description = "Configuration overrides for all nodes of a type or single nodes")
@Path("/system/configuration_overrides")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationOverridesResource extends RestResource {
    private final ConfigurationOverridesService configurationOverridesService;

    @Inject
    public ConfigurationOverridesResource(ConfigurationOverridesService configurationOverridesService) {
        this.configurationOverridesService = configurationOverridesService;
    }

    @GET
    @Timed
    @Operation(summary = "Get the configuration overrides for all nodes of a type or for a single node")
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_READ)
    public ConfigurationOverrides get(@Parameter(name = "node_type", description = "server or datanode", required = true)
                                      @QueryParam("node_type") @NotNull NodeType nodeType,
                                      @Parameter(name = "node_id", description = "Only the overrides of this node, instead of all nodes of the type")
                                      @QueryParam("node_id") @Nullable String nodeId) {
        final String scopeNodeId = emptyToNull(nodeId);
        return new ConfigurationOverrides(nodeType, scopeNodeId, configurationOverridesService.getOverrides(nodeType, scopeNodeId));
    }

    @PUT
    @Timed
    @Operation(summary = "Change the configuration overrides for all nodes of a type or for a single node",
               description = "Only the given overrides are changed, a null value removes an override.")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    @AuditEvent(type = AuditEventTypes.CONFIGURATION_OVERRIDES_UPDATE)
    public ConfigurationOverrides update(@Parameter(name = "body", required = true)
                                         @NotNull @Valid ConfigurationOverridesUpdate request) {
        if (request.overrides().keySet().stream().anyMatch(String::isBlank)) {
            throw new BadRequestException("Parameter names must not be empty");
        }
        if (request.overrides().values().stream().anyMatch(Objects::isNull)) {
            checkPermission(RestPermissions.CLUSTER_CONFIG_ENTRY_DELETE);
        }

        final String scopeNodeId = emptyToNull(request.nodeId());
        final Map<String, ConfigurationOverrideValue> overrides = configurationOverridesService.updateOverrides(
                request.nodeType(), scopeNodeId, request.overrides(), currentUserName());
        return new ConfigurationOverrides(request.nodeType(), scopeNodeId, overrides);
    }

    private String currentUserName() {
        final User user = getCurrentUser();
        return user != null ? user.getName() : String.valueOf(getSubject().getPrincipal());
    }

    /**
     * @param nodeId    The node the overrides apply to, {@code null} if they apply to all nodes of the type
     * @param overrides The overrides keyed and sorted by parameter name
     */
    public record ConfigurationOverrides(@JsonProperty("node_type") NodeType nodeType,
                                         @JsonProperty("node_id") @Nullable String nodeId,
                                         @JsonProperty("overrides") Map<String, ConfigurationOverrideValue> overrides) {}

    /**
     * @param nodeId    The node to change the overrides of, {@code null} to change them for all nodes of the type
     * @param overrides The new values keyed by parameter name, a {@code null} value removes the override
     */
    public record ConfigurationOverridesUpdate(@JsonProperty("node_type") @NotNull NodeType nodeType,
                                               @JsonProperty("node_id") @Nullable String nodeId,
                                               @JsonProperty("overrides") @NotNull Map<String, String> overrides) {}
}
