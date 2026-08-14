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
package org.graylog2.rest.resources.opensearch;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.nodes.opensearch.OpensearchNode;
import org.graylog2.cluster.nodes.opensearch.OpensearchNodesProvider;
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
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.utilities.lucene.InMemorySearchEngine;
import org.graylog2.utilities.lucene.LuceneInMemorySearchEngine;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Tag(name = "System/Opensearch", description = "OpenSearch Node discovery")
@RequiresAuthentication
@Path("/system/cluster/opensearch")
@Produces(MediaType.APPLICATION_JSON)
public class OpensearchClusterResource extends RestResource {

    private static final String DEFAULT_SORT_FIELD = "name";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id(OpensearchNode.FIELD_ID).title("ID").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).hidden(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_NAME).title("Node Name").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_VERSION).title("Version").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_ROLES).title("Roles").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_JVM_HEAP_MAX).title("JVM Heap Max").type(SearchQueryField.Type.LONG).sortable(true).hidden(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_JVM_HEAP_USED_PERCENT).title("JVM Heap").type(SearchQueryField.Type.DOUBLE).sortable(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_CPU_USED_PERCENT).title("CPU").type(SearchQueryField.Type.DOUBLE).sortable(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_DISK_USED_PERCENT).title("Disk").type(SearchQueryField.Type.DOUBLE).sortable(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_DISK_USED).title("Disk Used").type(SearchQueryField.Type.LONG).sortable(true).hidden(true).build(),
            EntityAttribute.builder().id(OpensearchNode.FIELD_DISK_TOTAL).title("Disk Total").type(SearchQueryField.Type.LONG).sortable(true).hidden(true).build()
    );

    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    private final InMemorySearchEngine<OpensearchNode> opensearchNodesSearchService;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public OpensearchClusterResource(OpensearchNodesProvider provider) {
        final Supplier<List<OpensearchNode>> cachingSupplier = Suppliers.memoizeWithExpiration(
                provider::get,
                10,
                TimeUnit.SECONDS
        );
        this.opensearchNodesSearchService = new LuceneInMemorySearchEngine<>(attributes, cachingSupplier);
        this.searchQueryParser = new SearchQueryParser(DEFAULT_SORT_FIELD, attributes);
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @Operation(summary = "Get a paginated list of all OpenSearch nodes in this cluster")
    public PageListResponse<OpensearchNode> listNodes(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                      @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                      @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                      @Parameter(name = "sort",
                                                                 description = "The field to sort the result on",
                                                                 required = true,
                                                                 schema = @Schema(allowableValues = {
                                                                         OpensearchNode.FIELD_ID,
                                                                         OpensearchNode.FIELD_NAME,
                                                                         OpensearchNode.FIELD_VERSION,
                                                                         OpensearchNode.FIELD_ROLES,
                                                                         OpensearchNode.FIELD_JVM_HEAP_MAX,
                                                                         OpensearchNode.FIELD_JVM_HEAP_USED_PERCENT,
                                                                         OpensearchNode.FIELD_CPU_USED_PERCENT,
                                                                         OpensearchNode.FIELD_DISK_USED_PERCENT,
                                                                         OpensearchNode.FIELD_DISK_USED,
                                                                         OpensearchNode.FIELD_DISK_TOTAL
                                                                 }))
                                                      @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                      @Parameter(name = "order", description = "The sort direction",
                                                                 schema = @Schema(allowableValues = {"asc", "desc"}))
                                                      @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order

    ) throws QueryNodeException, IOException {
        final SearchQuery parsedQuery = searchQueryParser.parse(query);
        final PaginatedList<OpensearchNode> result = opensearchNodesSearchService.search(parsedQuery, sort, order, page, perPage);
        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.stream().toList(), attributes, settings);
    }
}
