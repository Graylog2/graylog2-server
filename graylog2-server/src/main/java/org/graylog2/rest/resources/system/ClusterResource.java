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
import com.eaio.uuid.UUID;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.nodes.ServerNodeDto;
import org.graylog2.cluster.nodes.ServerNodePaginatedService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.system.cluster.responses.NodeSummary;
import org.graylog2.rest.models.system.cluster.responses.NodeSummaryList;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "System/Cluster", description = "Node discovery")
@RequiresAuthentication
@Path("/system/cluster")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterResource extends RestResource {
    private final NodeService nodeService;

    private final NodeId nodeId;
    private final ClusterId clusterId;

    private final ServerNodePaginatedService serverNodePaginatedService;
    private final SearchQueryParser searchQueryParser;

    public static final ImmutableMap<String, SearchQueryField> SERVER_NODE_ENTITY_SEARCH_MAPPINGS = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(ServerNodeDto.FIELD_HOSTNAME, SearchQueryField.create(ServerNodeDto.FIELD_HOSTNAME, SearchQueryField.Type.STRING))
            .put(ServerNodeDto.FIELD_NODE_ID, SearchQueryField.create(ServerNodeDto.FIELD_NODE_ID, SearchQueryField.Type.STRING))
            .put("short_node_id", SearchQueryField.create("short_node_id", SearchQueryField.Type.STRING))
            .put("transport_address", SearchQueryField.create("transport_address", SearchQueryField.Type.STRING))
            .build();

    private static final String DEFAULT_SORT_FIELD = "hostname";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id("is_leader").title("Leader").filterable(true).sortable(true).build(),
            EntityAttribute.builder().id("transport_address").title("Transport address").searchable(true).sortable(true).build(),
            EntityAttribute.builder().id("last_seen").title("Last seen").sortable(true).build(),
            EntityAttribute.builder().id("hostname").title("Node").searchable(true).sortable(true).build(),
            EntityAttribute.builder().id("node_id").title("Node ID").searchable(true).sortable(true).type(SearchQueryField.Type.STRING).build(),
            EntityAttribute.builder().id("short_node_id").title("Short node ID").sortable(true).build(),
            EntityAttribute.builder().id(ServerNodeDto.FIELD_LOAD_BALANCER_STATUS).title("Load balancer").sortable(true).filterable(true).filterOptions(loadBalancerOptions()).build(),
            EntityAttribute.builder().id(ServerNodeDto.FIELD_LIFECYCLE).title("Status").sortable(true).filterable(true).filterOptions(lifecycleOptions()).build(),
            EntityAttribute.builder().id(ServerNodeDto.FIELD_IS_PROCESSING).title("Processing").sortable(true).filterable(true).build()
    );

    private static Set<FilterOption> loadBalancerOptions() {
        return Arrays.stream(LoadBalancerStatus.values())
                .map(status -> new FilterOption(status.name(), status.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<FilterOption> lifecycleOptions() {
        return Arrays.stream(Lifecycle.values())
                .map(status -> new FilterOption(status.name(), status.getDescription()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    @Inject
    public ClusterResource(final NodeService nodeService,
                           final ClusterConfigService clusterConfigService,
                           final NodeId nodeId, ServerNodePaginatedService serverNodePaginatedService) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create(UUID.nilUUID().toString()));
        this.serverNodePaginatedService = serverNodePaginatedService;
        this.searchQueryParser = new SearchQueryParser(DEFAULT_SORT_FIELD, SERVER_NODE_ENTITY_SEARCH_MAPPINGS);
    }

    @GET
    @Timed
    @Path("/nodes")
    @Operation(summary = "List all active nodes in this cluster.")
    public NodeSummaryList nodes() {
        final Map<String, Node> nodes = nodeService.allActive();
        final List<NodeSummary> nodeList = new ArrayList<>(nodes.size());
        for (Node node : nodes.values()) {
            nodeList.add(nodeSummary(node));
        }

        return NodeSummaryList.create(nodeList);
    }

    @GET
    @Path("/nodes/paginated")
    @Timed
    @Operation(summary = "Get a paginated list of all server nodes in this cluster")
    public PageListResponse<ServerNodeDto> nodes(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                 @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                 @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                 @Parameter(name = "sort",
                                                           description = "The field to sort the result on",
                                                           required = true,
                                                           schema = @Schema(allowableValues = {"hostname", "node_id", "short_node_id", "transport_address"}))
                                                 @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                 @Parameter(name = "order", description = "The sort direction",
                                                           schema = @Schema(allowableValues = {"asc", "desc"}))
                                                 @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order

    ) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<ServerNodeDto> result = serverNodePaginatedService.searchPaginated(searchQuery, order.toBsonSort(sort), page, perPage);
        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.stream().toList(), attributes, settings);
    }

    @GET
    @Timed
    @Path("/node")
    @Operation(summary = "Information about this node.",
                  description = "This is returning information of this node in context to its state in the cluster. " +
                          "Use the system API of the node itself to get system information.")
    public NodeSummary node() throws NodeNotFoundException {
        return nodeSummary(nodeService.byNodeId(nodeId));
    }

    @GET
    @Timed
    @Path("/nodes/{nodeId}")
    @Operation(summary = "Information about a node.",
                  description = "This is returning information of a node in context to its state in the cluster. " +
                          "Use the system API of the node itself to get system information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns node information", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Node not found.")
    })
    public NodeSummary node(@Parameter(name = "nodeId", required = true) @PathParam("nodeId") @NotEmpty String nodeId) throws NodeNotFoundException {
        return nodeSummary(nodeService.byNodeId(nodeId));
    }

    private NodeSummary nodeSummary(Node node) {
        return NodeSummary.create(
                clusterId.clusterId(),
                node.getNodeId(),
                node.isLeader(),
                node.getTransportAddress(),
                Tools.getISO8601String(node.getLastSeen()),
                node.getShortNodeId(),
                node.getHostname()
        );
    }
}
