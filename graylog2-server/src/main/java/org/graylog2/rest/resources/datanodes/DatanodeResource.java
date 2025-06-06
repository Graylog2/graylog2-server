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
package org.graylog2.rest.resources.datanodes;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.List;
import java.util.Locale;

@Api(value = "System/Datanodes", description = "Data Node discovery")
@RequiresAuthentication
@Path("/system/cluster/datanodes")
@Produces(MediaType.APPLICATION_JSON)
public class DatanodeResource extends RestResource {

    private final DataNodePaginatedService dataNodePaginatedService;
    private final SearchQueryParser searchQueryParser;

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put("hostname", SearchQueryField.create("hostname"))
            .put("datanode_status", SearchQueryField.create("datanode_status"))
            .build();

    private static final String DEFAULT_SORT_FIELD = "title";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id("hostname").title("Name").build(),
            EntityAttribute.builder().id("data_node_status").title("Status").build(),
            EntityAttribute.builder().id("transport_address").title("Transport address").build(),
            EntityAttribute.builder().id("cert_valid_until").title("Certificate valid until").build(),
            EntityAttribute.builder().id("datanode_version").title("Datanode version").build()
    );
    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    @Inject
    public DatanodeResource(DataNodePaginatedService dataNodePaginatedService) {
        this.dataNodePaginatedService = dataNodePaginatedService;
        this.searchQueryParser = new SearchQueryParser("hostname", SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
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
                                                   @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order

    ) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<DataNodeDto> result = dataNodePaginatedService.searchPaginated(searchQuery, order.toBsonSort(sort), page, perPage);


        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.stream().toList(), attributes, settings);
    }

}
