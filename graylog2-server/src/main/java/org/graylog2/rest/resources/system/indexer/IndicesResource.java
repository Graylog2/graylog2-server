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
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.NodeInfoCache;
import org.graylog2.indexer.indexset.index.IndexPattern;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.indices.util.NumberBasedIndexNameComparator;
import org.graylog2.rest.models.system.indexer.requests.IndicesReadRequest;
import org.graylog2.rest.models.system.indexer.responses.AllIndices;
import org.graylog2.rest.models.system.indexer.responses.ClosedIndices;
import org.graylog2.rest.models.system.indexer.responses.IndexInfo;
import org.graylog2.rest.models.system.indexer.responses.OpenIndicesInfo;
import org.graylog2.rest.models.system.indexer.responses.ShardRouting;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "Indexer/Indices", description = "Index information")
@Path("/system/indexer/indices")
public class IndicesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesResource.class);

    private final Indices indices;
    private final NodeInfoCache nodeInfoCache;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public IndicesResource(Indices indices, NodeInfoCache nodeInfoCache, IndexSetRegistry indexSetRegistry) {
        this.indices = indices;
        this.nodeInfoCache = nodeInfoCache;
        this.indexSetRegistry = indexSetRegistry;
    }

    @GET
    @Timed
    @Path("/{index}")
    @Operation(summary = "Get information of an index and its shards.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexInfo single(@Parameter(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_READ, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        return indices.getIndexStats(index)
                .map(this::toIndexInfo)
                .orElseThrow(() -> new NotFoundException("Index [" + index + "] not found."));
    }

    @POST
    @Timed
    @Path("/multiple")
    @Operation(summary = "Get information of all specified indices and their shards.")
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to request index information")
    public List<IndexInfo> multiple(@Parameter(name = "Requested indices", required = true)
                                    @Valid @NotNull IndicesReadRequest request) {
        final List<String> requestedIndices = request.indices().stream()
                .filter(index -> isPermitted(RestPermissions.INDICES_READ, index))
                .distinct()
                .collect(Collectors.toList());
        final Map<String, Boolean> managedStatus = indexSetRegistry.isManagedIndex(requestedIndices);
        final List<String> managedIndices = requestedIndices.stream()
                .filter(index -> managedStatus.getOrDefault(index, false))
                .collect(Collectors.toList());

        return toIndexInfos(indices.getIndicesStats(managedIndices));
    }

    @GET
    @Path("/open")
    @Timed
    @Operation(summary = "Get information of all open indices managed by Graylog and their shards.")
    @RequiresPermissions(RestPermissions.INDICES_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public OpenIndicesInfo open() {
        final Set<IndexSet> indexSets = indexSetRegistry.getAllIndexSets();
        final Set<String> indexWildcards = indexSets.stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());
        final Set<IndexStatistics> indicesStats = indices.getIndicesStats(indexWildcards);

        return getOpenIndicesInfo(indicesStats);
    }

    @GET
    @Timed
    @Path("/closed")
    @Operation(summary = "Get a list of closed indices that can be reopened.")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices closed() {
        final Set<IndexSet> indexSets = indexSetRegistry.getAllIndexSets();
        final Set<String> indexWildcards = indexSets.stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());
        final Set<String> closedIndices = indices.getClosedIndices(indexWildcards).stream()
                .filter(index -> isPermitted(RestPermissions.INDICES_READ, index))
                .collect(Collectors.toSet());

        return ClosedIndices.create(closedIndices, closedIndices.size());
    }

    @GET
    @Timed
    @Path("/reopened")
    @Operation(summary = "Get a list of reopened indices, which will not be cleaned by retention cleaning")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices reopened() {
        final Set<IndexSet> indexSets = indexSetRegistry.getAllIndexSets();
        final Set<String> indexWildcards = indexSets.stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());
        final Set<String> reopenedIndices = indices.getReopenedIndices(indexWildcards).stream()
                .filter(index -> isPermitted(RestPermissions.INDICES_READ, index))
                .collect(Collectors.toSet());

        return ClosedIndices.create(reopenedIndices, reopenedIndices.size());
    }

    @GET
    @Timed
    @Operation(summary = "List all open, closed and reopened indices.")
    @Produces(MediaType.APPLICATION_JSON)
    public AllIndices all() {
        return AllIndices.create(this.closed(), this.reopened(), this.open());
    }

    @POST
    @Timed
    @Path("/{index}/reopen")
    @Operation(summary = "Reopen a closed index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ES_INDEX_OPEN)
    public void reopen(@Parameter(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        indices.reopenIndex(index);
    }

    @POST
    @Timed
    @Path("/{index}/close")
    @Operation(summary = "Close an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "403", description = "You cannot close the current deflector target index.")
    })
    @AuditEvent(type = AuditEventTypes.ES_INDEX_CLOSE)
    public void close(@Parameter(name = "index") @PathParam("index") @NotNull String index) throws TooManyAliasesException {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        if (indexSetRegistry.isCurrentWriteIndex(index)) {
            throw new ForbiddenException("The current deflector target index (" + index + ") cannot be closed");
        }

        indices.close(index);
    }

    @DELETE
    @Timed
    @Path("/{index}")
    @Operation(summary = "Delete an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "403", description = "You cannot delete the current deflector target index.")
    })
    @AuditEvent(type = AuditEventTypes.ES_INDEX_DELETE)
    public void delete(@Parameter(name = "index") @PathParam("index") @NotNull String index) throws TooManyAliasesException {
        checkPermission(RestPermissions.INDICES_DELETE, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        if (indexSetRegistry.isCurrentWriteIndex(index)) {
            throw new ForbiddenException("The current deflector target index (" + index + ") cannot be deleted");
        }

        indices.delete(index);
    }

    // Index set

    @GET
    @Timed
    @Path("/{indexSetId}/list")
    @Operation(summary = "List all open, closed and reopened indices.")
    @Produces(MediaType.APPLICATION_JSON)
    public AllIndices indexSetList(@Parameter(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        return AllIndices.create(this.indexSetClosed(indexSetId), this.indexSetReopened(indexSetId), this.indexSetOpen(indexSetId));
    }

    @GET
    @Path("/{indexSetId}/open")
    @Timed
    @Operation(summary = "Get information of all open indices managed by Graylog and their shards.")
    @RequiresPermissions(RestPermissions.INDICES_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public OpenIndicesInfo indexSetOpen(@Parameter(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);
        final Set<IndexStatistics> indicesInfos = indices.getIndicesStats(indexSet).stream()
                .filter(indexStats -> isPermitted(RestPermissions.INDICES_READ, indexStats.index()))
                .collect(Collectors.toSet());

        return getOpenIndicesInfo(indicesInfos);
    }

    @GET
    @Timed
    @Path("/{indexSetId}/closed")
    @Operation(summary = "Get a list of closed indices that can be reopened.")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices indexSetClosed(@Parameter(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        final Set<String> closedIndices = indices.getClosedIndices(indexSet).stream()
                .filter(index -> isPermitted(RestPermissions.INDICES_READ, index))
                .collect(Collectors.toSet());

        return ClosedIndices.create(closedIndices, closedIndices.size());
    }

    @GET
    @Timed
    @Path("/{indexSetId}/reopened")
    @Operation(summary = "Get a list of reopened indices, which will not be cleaned by retention cleaning")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices indexSetReopened(@Parameter(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        final Set<String> reopenedIndices = indices.getReopenedIndices(indexSet).stream()
                .filter(index -> isPermitted(RestPermissions.INDICES_READ, index))
                .collect(Collectors.toSet());

        return ClosedIndices.create(reopenedIndices, reopenedIndices.size());
    }

    private OpenIndicesInfo getOpenIndicesInfo(Set<IndexStatistics> indicesStatistics) {
        final List<IndexInfo> indexInfos = new LinkedList<>();
        final Set<String> indices = indicesStatistics.stream()
                .map(IndexStatistics::index)
                .collect(Collectors.toSet());
        final Map<String, Boolean> areReopened = this.indices.areReopened(indices);

        final List<IndexStatistics> sortedIndexStatistics = indicesStatistics.stream()
                .sorted(Comparator.comparing(IndexStatistics::index, new NumberBasedIndexNameComparator(IndexPattern.SEPARATOR)))
                .toList();

        for (IndexStatistics indexStatistics : sortedIndexStatistics) {
            final IndexInfo indexInfo = IndexInfo.create(
                    indexStatistics.index(),
                    indexStatistics.primaryShards(),
                    indexStatistics.allShards(),
                    fillShardRoutings(indexStatistics.routing()),
                    areReopened.get(indexStatistics.index()));

            indexInfos.add(indexInfo);
        }

        return OpenIndicesInfo.create(indexInfos);
    }

    private List<ShardRouting> fillShardRoutings(List<ShardRouting> shardRoutings) {
        return shardRoutings.stream()
                .map(shardRouting ->
                        shardRouting.withNodeDetails(
                                nodeInfoCache.getNodeName(shardRouting.nodeId()).orElse(null),
                                nodeInfoCache.getHostName(shardRouting.nodeId()).orElse(null))
                ).collect(Collectors.toList());
    }

    private IndexInfo toIndexInfo(final IndexStatistics indexStatistics) {
        return IndexInfo.create(
                indexStatistics.index(),
                indexStatistics.primaryShards(),
                indexStatistics.allShards(),
                fillShardRoutings(indexStatistics.routing()),
                indices.isReopened(indexStatistics.index())
        );
    }

    private List<IndexInfo> toIndexInfos(final Collection<IndexStatistics> indexStatistics) {
        final Set<String> indexNames = indexStatistics.stream().map(IndexStatistics::index).collect(Collectors.toSet());
        final Map<String, Boolean> reopenedStatus = indices.areReopened(indexNames);

        final ImmutableList.Builder<IndexInfo> indexInfos = ImmutableList.builder();
        for (IndexStatistics indexStats : indexStatistics) {
            final IndexInfo indexInfo = IndexInfo.create(
                    indexStats.index(),
                    indexStats.primaryShards(),
                    indexStats.allShards(),
                    fillShardRoutings(indexStats.routing()),
                    reopenedStatus.getOrDefault(indexStats.index(), false));
            indexInfos.add(indexInfo);
        }

        return indexInfos.build();
    }
}
