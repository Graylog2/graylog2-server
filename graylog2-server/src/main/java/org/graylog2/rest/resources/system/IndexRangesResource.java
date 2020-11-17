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
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangesResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

@RequiresAuthentication
@Api(value = "System/IndexRanges", description = "Index timeranges")
@Path("/system/indices/ranges")
public class IndexRangesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRangesResource.class);

    private final IndexRangeService indexRangeService;
    private final RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory;
    private final CreateNewSingleIndexRangeJob.Factory singleIndexRangeJobFactory;
    private final IndexSetRegistry indexSetRegistry;
    private final SystemJobManager systemJobManager;

    @Inject
    public IndexRangesResource(IndexRangeService indexRangeService,
                               RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory,
                               CreateNewSingleIndexRangeJob.Factory singleIndexRangeJobFactory,
                               IndexSetRegistry indexSetRegistry,
                               SystemJobManager systemJobManager) {
        this.indexRangeService = indexRangeService;
        this.rebuildIndexRangesJobFactory = rebuildIndexRangesJobFactory;
        this.singleIndexRangeJobFactory = singleIndexRangeJobFactory;
        this.indexSetRegistry = indexSetRegistry;
        this.systemJobManager = systemJobManager;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all index ranges")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexRangesResponse list() {
        final SortedSet<IndexRange> all = indexRangeService.findAll();
        final List<IndexRangeSummary> ranges = Lists.newArrayListWithCapacity(all.size());
        for (IndexRange range : all) {
            if (!isPermitted(RestPermissions.INDEXRANGES_READ, range.indexName())) {
                continue;
            }
            final IndexRangeSummary indexRange = IndexRangeSummary.create(
                    range.indexName(),
                    range.begin(),
                    range.end(),
                    range.calculatedAt(),
                    range.calculationDuration()
            );
            ranges.add(indexRange);
        }

        return IndexRangesResponse.create(ranges.size(), ranges);
    }

    @GET
    @Path("/{index: [a-z_0-9]+}")
    @Timed
    @ApiOperation(value = "Show single index range")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexRangeSummary show(
            @ApiParam(name = "index", value = "The name of the Graylog-managed Elasticsearch index", required = true)
            @PathParam("index") @NotEmpty String index) throws NotFoundException {
        if (!indexSetRegistry.isManagedIndex(index)) {
            throw new BadRequestException(index + " is not a Graylog-managed Elasticsearch index.");
        }
        checkPermission(RestPermissions.INDEXRANGES_READ, index);

        final IndexRange indexRange = indexRangeService.get(index);
        return IndexRangeSummary.create(
                indexRange.indexName(),
                indexRange.begin(),
                indexRange.end(),
                indexRange.calculatedAt(),
                indexRange.calculationDuration()
        );
    }

    @POST
    @Timed
    @Path("/rebuild")
    @RequiresPermissions(RestPermissions.INDEXRANGES_REBUILD)
    @ApiOperation(value = "Rebuild/sync index range information.",
            notes = "This triggers a systemjob that scans every index and stores meta information " +
                    "about what indices contain messages in what timeranges. It atomically overwrites " +
                    "already existing meta information.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Rebuild/sync systemjob triggered.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ES_INDEX_RANGE_UPDATE_JOB)
    public Response rebuild() {
        submitIndexRangesJob(indexSetRegistry.getAll());

        return Response.accepted().build();
    }

    @POST
    @Timed
    @Path("/index_set/{indexSetId}/rebuild")
    @RequiresPermissions(RestPermissions.INDEXRANGES_REBUILD)
    @ApiOperation(value = "Rebuild/sync index range information for the given index set.",
            notes = "This triggers a systemjob that scans every index in the given index set and stores meta information " +
                    "about what indices contain messages in what timeranges. It atomically overwrites " +
                    "already existing meta information.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Rebuild/sync systemjob triggered.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ES_INDEX_RANGE_UPDATE_JOB)
    public Response rebuildIndexSet(@ApiParam(name = "indexSetId") @PathParam("indexSetId") @NotBlank final String indexSetId) {
        final IndexSet indexSet = indexSetRegistry.get(indexSetId)
                .orElseThrow(() -> new javax.ws.rs.NotFoundException("Index set <" + indexSetId + "> not found!"));

        submitIndexRangesJob(Collections.singleton(indexSet));

        return Response.accepted().build();
    }

    @POST
    @Timed
    @Path("/{index: [a-z_0-9-]+}/rebuild")
    @ApiOperation(value = "Rebuild/sync index range information.",
            notes = "This triggers a system job that scans an index and stores meta information " +
                    "about what indices contain messages in what time ranges. It atomically overwrites " +
                    "already existing meta information.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Rebuild/sync system job triggered.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ES_INDEX_RANGE_UPDATE_JOB)
    public Response rebuildIndex(
            @ApiParam(name = "index", value = "The name of the Graylog-managed Elasticsearch index", required = true)
            @PathParam("index") @NotEmpty String index) {
        if (!indexSetRegistry.isManagedIndex(index)) {
            throw new BadRequestException(index + " is not a Graylog-managed Elasticsearch index.");
        }
        checkPermission(RestPermissions.INDEXRANGES_REBUILD, index);

        final SystemJob rebuildJob = singleIndexRangeJobFactory.create(indexSetRegistry.getAll(), index);
        try {
            this.systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            final String msg = "Concurrency level of this job reached: " + e.getMessage();
            LOG.error(msg);
            throw new ForbiddenException(msg, e);
        }

        return Response.accepted().build();
    }

    private void submitIndexRangesJob(final Set<IndexSet> indexSets) {
        final SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(indexSets);
        try {
            this.systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            final String errorMsg = "Concurrency level of this job reached: " + e.getMessage();
            LOG.error(errorMsg, e);
            throw new ForbiddenException(errorMsg);
        }
    }

}
