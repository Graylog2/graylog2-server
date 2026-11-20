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
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.configuration.managed.ConfigurationCatalog;
import org.graylog2.configuration.managed.ConfigurationParameter;
import org.graylog2.configuration.managed.ConfigurationSource;
import org.graylog2.configuration.managed.ManagedConfigurationEntry;
import org.graylog2.configuration.managed.ManagedConfigurationServices;
import org.graylog2.configuration.managed.NodeType;
import org.graylog2.configuration.managed.ParameterBacking;
import org.graylog2.configuration.managed.ParameterConstraints;
import org.graylog2.configuration.managed.ParameterScope;
import org.graylog2.configuration.managed.ParameterType;
import org.graylog2.configuration.managed.PendingRestarts;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.emptyToNull;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Reads and changes the configuration parameters which are exposed through the catalog.
 * <p>
 * Every request works on one scope, matching what a single page shows: one node, or all nodes of a type. A request
 * with a node ID covers the parameters set per node, a request without one covers the cluster wide parameters.
 * <p>
 * Values are raw strings, exactly as they would be written in a configuration file. A stored value always wins over
 * the configuration file, environment variables and system properties, but is only applied when the node reads it,
 * which for most parameters means at its next start.
 */
@RequiresAuthentication
@Tag(name = "System/ManagedConfiguration", description = "Configuration parameters of Graylog server and data nodes")
@Path("/system/managed_configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ManagedConfigurationResource extends RestResource {
    private final ConfigurationCatalog catalog;
    private final ManagedConfigurationServices configurationServices;
    private final PendingRestarts pendingRestarts;
    private final NodeService<DataNodeDto> dataNodeService;

    @Inject
    public ManagedConfigurationResource(ConfigurationCatalog catalog,
                                        ManagedConfigurationServices configurationServices,
                                        PendingRestarts pendingRestarts,
                                        NodeService<DataNodeDto> dataNodeService) {
        this.catalog = catalog;
        this.configurationServices = configurationServices;
        this.pendingRestarts = pendingRestarts;
        this.dataNodeService = dataNodeService;
    }

    @GET
    @Timed
    @Operation(summary = "Get the configuration parameters of a single node, or of all nodes of a type")
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_READ)
    public ManagedConfiguration get(@Parameter(name = "node_type", description = "server or datanode", required = true)
                                    @QueryParam("node_type") @NotNull NodeType nodeType,
                                    @Parameter(name = "node_id", description = "The node to read, omit for the parameters shared by all nodes of the type")
                                    @QueryParam("node_id") @Nullable String nodeId) {
        final String scopeNodeId = emptyToNull(nodeId);
        return read(nodeType, scopeNodeId);
    }

    @PUT
    @Timed
    @Operation(summary = "Change the configuration parameters of a single node, or of all nodes of a type",
               description = "Only the given parameters are changed, a null value removes a stored value.")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    @AuditEvent(type = AuditEventTypes.MANAGED_CONFIGURATION_UPDATE)
    public ManagedConfiguration update(@Parameter(name = "body", required = true)
                                       @NotNull @Valid ManagedConfigurationUpdate request) {
        final String scopeNodeId = emptyToNull(request.nodeId());
        final ParameterScope scope = scopeNodeId == null ? ParameterScope.CLUSTER : ParameterScope.NODE;

        if (request.values().values().stream().anyMatch(Objects::isNull)) {
            checkPermission(RestPermissions.CLUSTER_CONFIG_ENTRY_DELETE);
        }
        request.values().forEach((name, value) -> validate(request.nodeType(), scope, name, value));

        configurationServices.get(request.nodeType())
                .update(scopeNodeId, request.values(), currentUserName());
        return read(request.nodeType(), scopeNodeId);
    }

    private void validate(NodeType nodeType, ParameterScope scope, String name, @Nullable String value) {
        final ConfigurationParameter parameter = catalog.get(nodeType, name)
                .orElseThrow(() -> new BadRequestException(
                        f("Unknown configuration parameter <%s> for node type <%s>.", name, nodeType.value())));
        if (parameter.scope() != scope) {
            throw new BadRequestException(f(
                    "Configuration parameter <%s> is set per %s, so %s a node ID.",
                    name,
                    parameter.scope().value(),
                    parameter.scope() == ParameterScope.NODE ? "the request needs" : "the request must not contain"));
        }
        if (value != null) {
            parameter.validate(value).ifPresent(message -> {
                throw new BadRequestException(message);
            });
        }
    }

    private ManagedConfiguration read(NodeType nodeType, @Nullable String nodeId) {
        final ParameterScope scope = nodeId == null ? ParameterScope.CLUSTER : ParameterScope.NODE;
        final Map<String, ManagedConfigurationEntry> entries = configurationServices.get(nodeType).get(nodeId);
        final Map<String, Set<String>> pendingByNode = pendingParametersByNode(nodeType, nodeId);
        final Set<String> pendingParameters = pendingByNode.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        final List<String> pendingNodes = pendingByNode.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();

        final List<ManagedConfigurationParameter> parameters = catalog.get(nodeType, scope).stream()
                .map(parameter -> ManagedConfigurationParameter.of(parameter, entries.get(parameter.name()),
                        pendingParameters.contains(parameter.name())))
                .toList();
        return new ManagedConfiguration(nodeType, nodeId, parameters, !pendingNodes.isEmpty(), pendingNodes,
                List.copyOf(restartingNodes(nodeType, nodeId)));
    }

    private Set<String> restartingNodes(NodeType nodeType, @Nullable String nodeId) {
        if (nodeType != NodeType.DATANODE) {
            return Set.of();
        }
        final Collection<String> nodeIds = nodeId == null ? dataNodeService.allActive().keySet() : List.of(nodeId);
        return pendingRestarts.restartingNodes(nodeType, nodeIds);
    }

    /**
     * The parameters which aren't in use yet, for the node of the request or for every active node of the type.
     * Restarts are only tracked for data nodes so far.
     */
    private Map<String, Set<String>> pendingParametersByNode(NodeType nodeType, @Nullable String nodeId) {
        if (nodeType != NodeType.DATANODE) {
            return Map.of();
        }
        final Collection<String> nodeIds = nodeId == null ? dataNodeService.allActive().keySet() : List.of(nodeId);
        return pendingRestarts.pendingParameters(nodeType, nodeIds);
    }

    private String currentUserName() {
        final User user = getCurrentUser();
        return user != null ? user.getName() : String.valueOf(getSubject().getPrincipal());
    }

    /**
     * @param nodeId          {@code null} when the parameters are shared by all nodes of the type
     * @param restartRequired Whether a node has to be restarted to use all stored values. For a single node this
     *                        also covers the values shared by all nodes of the type, which aren't part of
     *                        {@code parameters} then.
     * @param pendingNodes    The IDs of the nodes which have to be restarted
     * @param restartingNodes The IDs of the nodes which are restarting right now
     */
    public record ManagedConfiguration(@JsonProperty("node_type") NodeType nodeType,
                                       @JsonProperty("node_id") @Nullable String nodeId,
                                       @JsonProperty("parameters") List<ManagedConfigurationParameter> parameters,
                                       @JsonProperty("restart_required") boolean restartRequired,
                                       @JsonProperty("pending_nodes") List<String> pendingNodes,
                                       @JsonProperty("restarting_nodes") List<String> restartingNodes) {}

    /**
     * A parameter with everything needed to show and edit it.
     *
     * @param value      The value which applies, which is {@link #setValue()} when one has been stored
     * @param source     Where {@link #value} comes from, {@code null} while nothing is known about it yet
     * @param setValue   The value stored through this API, {@code null} if nobody stored one
     * @param nodeValue  What the node resolved for itself and runs with. Falls back to the default of the parameter
     *                   while no node reported a value: parameters which don't exist in a configuration bean are
     *                   never reported, and nodes report their own values, not the ones shared by all nodes.
     * @param pendingRestart Whether a node still has to be restarted to use the stored value
     */
    public record ManagedConfigurationParameter(@JsonProperty("name") String name,
                                                @JsonProperty("label") String label,
                                                @JsonProperty("description") String description,
                                                @JsonProperty("category") String category,
                                                @JsonProperty("order") int order,
                                                @JsonProperty("type") ParameterType type,
                                                @JsonProperty("units") Map<String, String> units,
                                                @JsonProperty("constraints") ParameterConstraints constraints,
                                                @JsonProperty("requires_restart") boolean requiresRestart,
                                                @JsonProperty("backing") ParameterBacking backing,
                                                @JsonProperty("scope") ParameterScope scope,
                                                @JsonProperty("value") @Nullable String value,
                                                @JsonProperty("source") @Nullable ConfigurationSource source,
                                                @JsonProperty("set_value") @Nullable String setValue,
                                                @JsonProperty("set_at") @Nullable Instant setAt,
                                                @JsonProperty("set_by") @Nullable String setBy,
                                                @JsonProperty("node_value") @Nullable String nodeValue,
                                                @JsonProperty("node_source") @Nullable ConfigurationSource nodeSource,
                                                @JsonProperty("pending_restart") boolean pendingRestart) {

        static ManagedConfigurationParameter of(ConfigurationParameter parameter,
                                                @Nullable ManagedConfigurationEntry entry,
                                                boolean pendingRestart) {
            final String reportedValue = entry == null ? null : entry.nodeValue();
            final String nodeValue = reportedValue != null ? reportedValue : parameter.defaultValue();
            final ConfigurationSource nodeSource = reportedValue != null
                    ? entry.nodeSource()
                    : (nodeValue == null ? null : ConfigurationSource.DEFAULT);
            final String storedValue = entry == null ? null : entry.value();

            return new ManagedConfigurationParameter(
                    parameter.name(),
                    parameter.label(),
                    parameter.description(),
                    parameter.category(),
                    parameter.order(),
                    parameter.type(),
                    parameter.units(),
                    parameter.constraints(),
                    parameter.requiresRestart(),
                    parameter.backing(),
                    parameter.scope(),
                    storedValue != null ? storedValue : nodeValue,
                    storedValue != null ? ConfigurationSource.DATABASE : nodeSource,
                    storedValue,
                    entry == null ? null : entry.updatedAt(),
                    entry == null ? null : entry.updatedBy(),
                    nodeValue,
                    nodeSource,
                    pendingRestart);
        }
    }

    /**
     * @param nodeId The node to change, {@code null} to change the parameters shared by all nodes of the type
     * @param values The new values keyed by parameter name, a {@code null} value removes the stored value
     */
    public record ManagedConfigurationUpdate(@JsonProperty("node_type") @NotNull NodeType nodeType,
                                             @JsonProperty("node_id") @Nullable String nodeId,
                                             @JsonProperty("values") @NotNull Map<String, String> values) {}
}
