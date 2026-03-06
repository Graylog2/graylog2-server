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
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.Document;
import org.graylog.security.certutil.audit.CaAuditEventTypes;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.cluster.nodes.mongodb.MongodbClusterCommand;
import org.graylog2.cluster.nodes.mongodb.MongodbNode;
import org.graylog2.cluster.nodes.mongodb.MongodbNodeUtils;
import org.graylog2.cluster.nodes.mongodb.MongodbNodesProvider;
import org.graylog2.cluster.nodes.mongodb.MongodbPermissionException;
import org.graylog2.cluster.nodes.mongodb.ProfilingLevel;
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
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.utilities.lucene.InMemorySearchEngine;
import org.graylog2.utilities.lucene.LuceneInMemorySearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "System/Mongodb", description = "MongoDB Node discovery")
@RequiresAuthentication
@Path("/system/cluster/mongodb")
@Produces(MediaType.APPLICATION_JSON)
public class MongodbClusterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(MongodbClusterResource.class);
    private static final String DEFAULT_SORT_FIELD = "name";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id(MongodbNode.FIELD_ID).title("ID").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_NAME).title("Node Name").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_ROLE).title("Role").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_VERSION).title("Version").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_PROFILING_LEVEL).title("Profiling Level").type(SearchQueryField.Type.STRING).sortable(true).filterable(true).filterOptions(profilingLevelOptions()).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_REPLICATION_LAG).title("Replication Lag").type(SearchQueryField.Type.LONG).sortable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_SLOW_QUERY_COUNT).title("Slow Query Count").type(SearchQueryField.Type.LONG).sortable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_STORAGE_USED_PERCENT).title("Storage Used").type(SearchQueryField.Type.DOUBLE).sortable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_AVAILABLE_CONNECTIONS).title("Connections available").type(SearchQueryField.Type.INT).sortable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_CURRENT_CONNECTIONS).title("Connections current").type(SearchQueryField.Type.INT).sortable(true).build(),
            EntityAttribute.builder().id(MongodbNode.FIELD_CONNECTIONS_USED_PERCENT).title("Connections used").type(SearchQueryField.Type.DOUBLE).sortable(true).build()
    );

    private static Set<FilterOption> profilingLevelOptions() {
        return Arrays.stream(ProfilingLevel.values()).map(level -> new FilterOption(level.name(), level.name())).collect(Collectors.toSet());
    }

    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();


    private final InMemorySearchEngine<MongodbNode> mongodbNodesSearchService;
    private final SearchQueryParser searchQueryParser;
    private final MongodbClusterCommand clusterCommand;

    @Inject
    public MongodbClusterResource(MongodbNodesProvider provider, MongodbClusterCommand clusterCommand) {
        this.clusterCommand = clusterCommand;
        final Supplier<List<MongodbNode>> cachingSupplier = Suppliers.memoizeWithExpiration(
                provider::get,
                10,
                TimeUnit.SECONDS
        );
        this.mongodbNodesSearchService = new LuceneInMemorySearchEngine<>(attributes, cachingSupplier);
        this.searchQueryParser = new SearchQueryParser(DEFAULT_SORT_FIELD, attributes);
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
        final SearchQuery parsedQuery = searchQueryParser.parse(query);
        final PaginatedList<MongodbNode> result = mongodbNodesSearchService.search(parsedQuery, sort, order, page, perPage);
        return PageListResponse.create(query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.stream().toList(), attributes, settings);
    }

    @GET
    @Path("/profiling/{level}")
    @AuditEvent(type = AuditEventTypes.MONGODB_ENABLE_PROFILING)
    @Operation(summary = "Enables profiling for all mongodb nodes")
    @RequiresPermissions(RestPermissions.MONGODB_ENABLE_PROFILING)
    public Response enableProfiling(@PathParam("level") ProfilingLevel level) {
        try {
            Document command = new Document("profile", level.getNumericalValue())
                    .append("slowms", MongodbNodeUtils.SLOW_QUERIES_THRESHOLD);
            clusterCommand.runOnEachNode(command);
            return profilingStatus();
        } catch (MongodbPermissionException e) {
            LOG.error("Failed to enable profiling due to insufficient MongoDB permissions", e);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(
                            "error", "Permission denied",
                            "message", e.getMessage(),
                            "hint", "The MongoDB user needs the 'enableProfiler' privilege on the database. Grant it with: db.grantRolesToUser('<username>', [{ role: 'dbAdmin', db: 'graylog' }])"
                    ))
                    .build();
        }
    }

    @GET
    @Path("/profiling/status")
    @Operation(summary = "Aggregates profiling status for all mongodb nodes")
    @Timed
    public Response profilingStatus() {
        try {
            Document command = new Document("profile", -1);
            final Map<ProfilingLevel, Long> result = clusterCommand.runOnEachNode(command)
                    .values().stream()
                    .map(doc -> doc.getInteger("was"))
                    .map(ProfilingLevel::fromNumericalValue)
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.counting()
                    ));
            return Response.ok().entity(result).build();
        } catch (MongodbPermissionException e) {
            LOG.error("Failed to retrieve profiling status due to insufficient MongoDB permissions", e);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(
                            "error", "Permission denied",
                            "message", e.getMessage(),
                            "hint", "The MongoDB user needs the 'clusterMonitor' or 'dbAdmin' role to read profiling status."
                    ))
                    .build();
        }
    }
}
