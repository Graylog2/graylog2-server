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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.indexer.indices.OutdatedIndex;
import org.graylog2.indexer.indices.OutdatedIndexService;
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
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.utilities.lucene.InMemorySearchEngine;
import org.graylog2.utilities.lucene.LuceneInMemorySearchEngine;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Tag(name = "System/Indexer/Indices", description = "Outdated index discovery")
@RequiresAuthentication
@Path("/system/indexer/indices/outdated")
@Produces(MediaType.APPLICATION_JSON)
public class OutdatedIndexResource extends RestResource {

    private static final String DEFAULT_SORT_FIELD = OutdatedIndex.FIELD_INDEX_NAME;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id(OutdatedIndex.FIELD_INDEX_NAME).title("Index Name").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(OutdatedIndex.FIELD_VERSION).title("Version").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(OutdatedIndex.FIELD_ACTIVE_WRITE_INDEX).title("Active Write Index").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(OutdatedIndex.FIELD_WARM_INDEX).title("Warm Index").type(SearchQueryField.Type.BOOLEAN).sortable(true).filterable(true).filterOptions(booleanFilterOptions()).build(),
            EntityAttribute.builder().id(OutdatedIndex.FIELD_MANAGED_INDEX).title("Managed Index").type(SearchQueryField.Type.BOOLEAN).sortable(true).filterable(true).filterOptions(booleanFilterOptions()).build(),
            EntityAttribute.builder().id(OutdatedIndex.FIELD_SYSTEM_INDEX).title("System Index").type(SearchQueryField.Type.BOOLEAN).sortable(true).filterable(true).filterOptions(booleanFilterOptions()).build(),
            EntityAttribute.builder().id(OutdatedIndex.FIELD_BEGIN).title("Message Range Begin").type(SearchQueryField.Type.DATE).sortable(true).build(),
            EntityAttribute.builder().id(OutdatedIndex.FIELD_END).title("Message Range End").type(SearchQueryField.Type.DATE).sortable(true).build()
    );

    // Booleans are indexed as int 1/0 (see OutdatedIndex#buildLuceneDoc and LuceneQueryBuilder), so the filter
    // values must be the numeric representations rather than "true"/"false".
    private static Set<FilterOption> booleanFilterOptions() {
        return Set.of(
                FilterOption.create("1", "True"),
                FilterOption.create("0", "False")
        );
    }

    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    private final OutdatedIndexService outdatedIndexService;
    private final InMemorySearchEngine<OutdatedIndex> outdatedIndexSearchService;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public OutdatedIndexResource(OutdatedIndexService outdatedIndexService) {
        this.outdatedIndexService = outdatedIndexService;
        final Supplier<List<OutdatedIndex>> cachingSupplier = Suppliers.memoizeWithExpiration(
                outdatedIndexService::getOutdatedIndices,
                10,
                TimeUnit.SECONDS
        );
        this.outdatedIndexSearchService = new LuceneInMemorySearchEngine<>(attributes, cachingSupplier);
        this.searchQueryParser = new SearchQueryParser(DEFAULT_SORT_FIELD, attributes);
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.INDICES_READ)
    @Operation(summary = "Get a list of indices that were created in an OpenSearch version prior to the recent one")
    public List<OutdatedIndex> getOutdatedIndices() {
        return outdatedIndexService.getOutdatedIndices();
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.INDICES_READ)
    @Operation(summary = "Get a paginated list of indices that were created in an OpenSearch version prior to the recent one")
    @Path("/paginated")
    public PageListResponse<OutdatedIndex> listOutdatedIndices(
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "sort",
                       description = "The field to sort the result on",
                       required = true,
                       schema = @Schema(allowableValues = {
                               OutdatedIndex.FIELD_INDEX_NAME,
                               OutdatedIndex.FIELD_VERSION,
                               OutdatedIndex.FIELD_ACTIVE_WRITE_INDEX,
                               OutdatedIndex.FIELD_WARM_INDEX,
                               OutdatedIndex.FIELD_MANAGED_INDEX,
                               OutdatedIndex.FIELD_SYSTEM_INDEX,
                               OutdatedIndex.FIELD_BEGIN,
                               OutdatedIndex.FIELD_END
                       }))
            @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order
    ) throws QueryNodeException, IOException {
        final SearchQuery parsedQuery = searchQueryParser.parse(query);
        final PaginatedList<OutdatedIndex> result = outdatedIndexSearchService.search(parsedQuery, sort, order, page, perPage);
        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.stream().toList(), attributes, settings);
    }

    @POST
    @Path("/{index}/reindex")
    @Operation(summary = "Reindexes an outdated index to make it compatible with the next major version of OpenSearch")
    @RequiresPermissions(RestPermissions.INDICES_REINDEX)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ES_INDEX_REINDEX)
    public void reindex(@Parameter(name = "index") @PathParam("index") @NotNull String index,
                        @Parameter(name = "withReplication") @QueryParam("withReplication") @DefaultValue("true") boolean withReplication) {
        OutdatedIndex outdatedIndex = getOutdatedIndices().stream()
                .filter(OutdatedIndex::isSystemIndex)
                .filter(i -> i.indexName().equals(index))
                .findAny().orElseThrow(() -> new NotFoundException("Index " + index + " not found or is no system index"));
        outdatedIndexService.reindex(outdatedIndex.indexName(), withReplication);
    }

    @DELETE
    @Path("/{index}")
    @Operation(summary = "Deletes an outdated, non-Graylog managed index")
    @RequiresPermissions(RestPermissions.INDICES_DELETE)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ES_INDEX_DELETE)
    public void deleteOutdated(@Parameter(name = "index") @PathParam("index") @NotNull String index) {
        OutdatedIndex outdatedIndex = getOutdatedIndices().stream()
                .filter(i -> !i.managedIndex())
                .filter(i -> i.indexName().equals(index))
                .findAny().orElseThrow(() -> new NotFoundException("Index " + index + " not found or is an index managed by Graylog"));
        outdatedIndexService.delete(outdatedIndex.indexName());
    }
}
