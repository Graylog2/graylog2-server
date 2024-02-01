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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.security.certutil.CertRenewalService;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodePaginatedService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.cluster.responses.NodeSummary;
import org.graylog2.rest.models.system.cluster.responses.NodeSummaryList;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.inject.Inject;

import jakarta.validation.constraints.NotEmpty;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Api(value = "System/Cluster", description = "Node discovery")
@RequiresAuthentication
@Path("/system/cluster")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterResource extends RestResource {
    private final NodeService nodeService;
    private final DataNodePaginatedService dataNodePaginatedService;
    private final SearchQueryParser searchQueryParser;
    private final CertRenewalService certRenewalService;

    private final NodeId nodeId;
    private final ClusterId clusterId;

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put("hostname", SearchQueryField.create("hostname"))
            .build();

    private static final String DEFAULT_SORT_FIELD = "title";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id("hostname").title("Name").build()
    );
    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    @Inject
    public ClusterResource(final NodeService nodeService,
                           final DataNodePaginatedService dataNodePaginatedService,
                           CertRenewalService certRenewalService, final ClusterConfigService clusterConfigService,
                           final NodeId nodeId) {
        this.nodeService = nodeService;
        this.dataNodePaginatedService = dataNodePaginatedService;
        this.certRenewalService = certRenewalService;
        this.searchQueryParser = new SearchQueryParser("hostname", SEARCH_FIELD_MAPPING);
        this.nodeId = nodeId;
        this.clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create(UUID.nilUUID().toString()));
    }

    @GET
    @Timed
    @Path("/nodes")
    @ApiOperation(value = "List all active nodes in this cluster.")
    public NodeSummaryList nodes() {
        final Map<String, Node> nodes = nodeService.allActive();
        final List<NodeSummary> nodeList = new ArrayList<>(nodes.size());
        for (Node node : nodes.values()) {
            nodeList.add(nodeSummary(node));
        }

        return NodeSummaryList.create(nodeList);
    }

    @GET
    @Timed
    @Path("/datanodes")
    @ApiOperation(value = "Get a paginated list of all datanodes in this cluster")
    public PageListResponse<DataNodeDto> dataNodes(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                   @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                   @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                   @ApiParam(name = "sort",
                                                             value = "The field to sort the result on",
                                                             required = true,
                                                             allowableValues = "title,description,type")
                                                   @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                   @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                   @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") String order

    ) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<DataNodeDto> result = dataNodePaginatedService.searchPaginated(searchQuery, sort, order, page, perPage);

        final List<DataNodeDto> dataNodes = certRenewalService.addProvisioningInformation(result.delegate());

        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, dataNodes, attributes, settings);
    }

    @GET
    @Timed
    @Path("/node")
    @ApiOperation(value = "Information about this node.",
                  notes = "This is returning information of this node in context to its state in the cluster. " +
                          "Use the system API of the node itself to get system information.")
    public NodeSummary node() throws NodeNotFoundException {
        return nodeSummary(nodeService.byNodeId(nodeId));
    }

    @GET
    @Timed
    @Path("/nodes/{nodeId}")
    @ApiOperation(value = "Information about a node.",
                  notes = "This is returning information of a node in context to its state in the cluster. " +
                          "Use the system API of the node itself to get system information.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Node not found.")
    })
    public NodeSummary node(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") @NotEmpty String nodeId) throws NodeNotFoundException {
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
