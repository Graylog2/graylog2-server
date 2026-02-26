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
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.nodes.mongodb.MongodbNode;
import org.graylog2.utilities.lucene.InMemorySearchEngine;
import org.graylog2.utilities.lucene.LuceneInMemorySearchEngine;
import org.graylog2.cluster.nodes.mongodb.MongodbNodesProvider;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.rest.resources.RestResource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Tag(name = "System/Mongodb", description = "MongoDB Node discovery")
@RequiresAuthentication
@Path("/system/cluster/mongodb")
@Produces(MediaType.APPLICATION_JSON)
public class MongodbClusterResource extends RestResource {

    private static final String DEFAULT_SORT_FIELD = "name";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id("id").title("ID").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id("name").title("Node Name").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id("role").title("Role").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id("version").title("Version").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id("status").title("Status").type(SearchQueryField.Type.INT).sortable(true).build(),
            EntityAttribute.builder().id("replicationLag").title("Replication Lag (ms)").type(SearchQueryField.Type.LONG).sortable(true).build(),
            EntityAttribute.builder().id("slowQueryCount").title("Slow Query Count").type(SearchQueryField.Type.LONG).sortable(true).build(),
            EntityAttribute.builder().id("storageUsedPercent").title("Storage Used (%)").type(SearchQueryField.Type.DOUBLE).sortable(true).build()
    );

    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();


    private final InMemorySearchEngine<MongodbNode> mongodbNodesSearchService;

    @Inject
    public MongodbClusterResource(Set<MongodbNodesProvider> providers) {
        this.mongodbNodesSearchService = new LuceneInMemorySearchEngine<>(DEFAULT_SORT_FIELD, attributes, () -> retrieveMongodbNodes(providers));
    }

    private List<MongodbNode> retrieveMongodbNodes(Set<MongodbNodesProvider> providers) {
        return providers.stream()
                .filter(MongodbNodesProvider::available)
                .findFirst()
                .map(MongodbNodesProvider::allNodes)
                .orElseThrow(() -> new IllegalStateException("No available Mongodb nodes"));
    }


    @GET
    @Timed
    @Operation(summary = "Get a paginated list of all MongoDB nodes in this cluster")
    public PageListResponse<MongodbNode> dataNodes(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                   @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                   @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                   @Parameter(name = "sort",
                                                              description = "The field to sort the result on",
                                                              required = true,
                                                              schema = @Schema(allowableValues = {"name", "role", "version", "status", "cpuUsage", "memoryUsage", "replicationLag", "slowQueryCount", "storageUsedPercent"}))
                                                   @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                   @Parameter(name = "order", description = "The sort direction",
                                                              schema = @Schema(allowableValues = {"asc", "desc"}))
                                                   @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order

    ) throws QueryNodeException, IOException {
        final PaginatedList<MongodbNode> result = mongodbNodesSearchService.search(query, sort, order, page, perPage);
        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.stream().toList(), attributes, settings);
    }
}
