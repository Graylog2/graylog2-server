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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.management.allocation.AllocationService;
import org.graylog2.indexer.management.allocation.ShardExplanation;
import org.graylog2.indexer.management.allocation.ShardMap;
import org.graylog2.indexer.management.allocation.UnassignedShard;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

@RequiresAuthentication
@Tag(name = "Indexer/Indices/Management/Allocation", description = "Index Management: why shards are unassigned")
@Path("/system/indexer/management/allocation")
@Produces(MediaType.APPLICATION_JSON)
public class IndexAllocationResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndexAllocationResource.class);
    // One explain call per shard; a badly broken cluster can have thousands of unassigned shards.
    private static final int MAX_EXPLAINED = 50;

    private final AllocationService allocationService;

    @Inject
    public IndexAllocationResource(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GET
    @Timed
    @Path("/explain")
    @Operation(summary = "Explain every unassigned shard (up to 50), parsed down to the essentials.")
    public ShardExplanation.Response explain() {
        checkPermission(RestPermissions.INDEXERCLUSTER_READ);

        final List<UnassignedShard> unassigned = allocationService.unassignedShards().stream()
                .filter(shard -> isPermitted(RestPermissions.INDICES_READ, shard.index()))
                .toList();

        final List<ShardExplanation> explained = unassigned.stream()
                .limit(MAX_EXPLAINED)
                .map(shard -> {
                    try {
                        return allocationService.explain(shard);
                    } catch (Exception e) {
                        LOG.debug("Explaining {}[{}] failed", shard.index(), shard.shard(), e);
                        return ShardExplanation.failed(shard, e.getMessage());
                    }
                })
                .toList();

        return new ShardExplanation.Response(
                unassigned.size(),
                (int) unassigned.stream().filter(UnassignedShard::primary).count(),
                explained,
                unassigned.size() > MAX_EXPLAINED,
                Instant.now().toString());
    }

    @GET
    @Timed
    @Path("/map")
    @Operation(summary = "Every node and shard copy, unassigned copies with a quick diagnosis (no explain calls).")
    public ShardMap map() {
        checkPermission(RestPermissions.INDEXERCLUSTER_READ);

        final ShardMap map = allocationService.shardMap();
        return new ShardMap(map.nodes(),
                map.shards().stream().filter(copy -> isPermitted(RestPermissions.INDICES_READ, copy.index())).toList(),
                map.generatedAt());
    }

    @GET
    @Timed
    @Path("/explain/{index}/{shard}")
    @Operation(summary = "Explain one shard copy, e.g. the one opened on the map.")
    public ShardExplanation explainOne(@Parameter(name = "index") @PathParam("index") String index,
                                       @Parameter(name = "shard") @PathParam("shard") int shard,
                                       @Parameter(name = "primary") @QueryParam("primary") @DefaultValue("true") boolean primary) throws Exception {
        checkPermission(RestPermissions.INDEXERCLUSTER_READ);
        checkPermission(RestPermissions.INDICES_READ, index);
        return allocationService.explain(new UnassignedShard(index, shard, primary, null, null));
    }

    @POST
    @Timed
    @Path("/retry_failed")
    @Operation(summary = "Retry shard allocations that hit the retry limit (_cluster/reroute?retry_failed=true).")
    @AuditEvent(type = AuditEventTypes.ES_CLUSTER_REROUTE_RETRY_FAILED)
    public RetryResponse retryFailed() {
        // Cluster-wide: needs indices:changestate for all indices, not just some.
        checkPermission(RestPermissions.INDICES_CHANGESTATE);
        return new RetryResponse(allocationService.retryFailedAllocations());
    }

    public record RetryResponse(@JsonProperty("acknowledged") boolean acknowledged) {
    }
}
