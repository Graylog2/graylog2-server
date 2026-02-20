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
package org.graylog2.rest.resources.mongodb;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodePaginatedService;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.MongodbNode;
import org.graylog2.cluster.nodes.MongodbNodesService;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "System/Mongodb", description = "MongoDB Node discovery")
@RequiresAuthentication
@Path("/system/cluster/mongodb")
@Produces(MediaType.APPLICATION_JSON)
public class MongodbClusterResource extends RestResource {

    private final MongodbNodesService mongodbNodesService;
    private final SearchQueryParser searchQueryParser;

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(DataNodeDto.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(DataNodeDto.FIELD_HOSTNAME, SearchQueryField.create(DataNodeDto.FIELD_HOSTNAME))
            .put(DataNodeDto.FIELD_DATANODE_STATUS, SearchQueryField.create(DataNodeDto.FIELD_DATANODE_STATUS))
            .build();

    private static final String DEFAULT_SORT_FIELD = DataNodeDto.FIELD_HOSTNAME;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id(DataNodeDto.FIELD_HOSTNAME).title("Node").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(DataNodeDto.FIELD_DATANODE_STATUS).sortable(true).filterable(true).filterOptions(danodeStatusOptions()).title("Status").build(),
            EntityAttribute.builder().id(DataNodeDto.FIELD_CLUSTER_ADDRESS).title("Transport address").type(SearchQueryField.Type.STRING).searchable(true).sortable(true).build(),
            EntityAttribute.builder().id(DataNodeDto.FIELD_CERT_VALID_UNTIL).title("Certificate valid until").type(SearchQueryField.Type.DATE).sortable(true).build(),
            EntityAttribute.builder().id(DataNodeDto.FIELD_DATANODE_VERSION).title("Version").type(SearchQueryField.Type.STRING).sortable(true).build(),
            EntityAttribute.builder().id(DataNodeDto.FIELD_OPENSEARCH_ROLES).title("Roles").type(SearchQueryField.Type.STRING).sortable(true).build()
    );

    private static Set<FilterOption> danodeStatusOptions() {
        return Arrays.stream(DataNodeStatus.values()).map(s -> new FilterOption(s.name(), s.name())).collect(Collectors.toSet());
    }

    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    @Inject
    public MongodbClusterResource(MongodbNodesService mongodbNodesService) {
        this.mongodbNodesService = mongodbNodesService;
        this.searchQueryParser = new SearchQueryParser("hostname", SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @Operation(summary = "Get a paginated list of all datanodes in this cluster")
    public PageListResponse<MongodbNode> dataNodes(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                   @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                   @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                   @Parameter(name = "sort",
                                                             description = "The field to sort the result on",
                                                             required = true,
                                                             schema = @Schema(allowableValues = {"hostname", "data_node_status", "transport_address", "cert_valid_until", "datanode_version"}))
                                                   @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                   @Parameter(name = "order", description = "The sort direction",
                                                             schema = @Schema(allowableValues = {"asc", "desc"}))
                                                   @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order

    ) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<MongodbNode> result = mongodbNodesService.searchPaginated(searchQuery, order.toBsonSort(sort), page, perPage);


        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.stream().toList(), attributes, settings);
    }

}
