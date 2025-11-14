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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.rest.models.system.indexer.responses.IndexerOverview;
import org.graylog2.rest.resources.system.DeflectorResource;
import org.graylog2.rest.resources.system.IndexRangesResource;
import org.graylog2.shared.rest.resources.RestResource;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Indexer/Overview", description = "Indexing overview", tags = {CLOUD_VISIBLE})
@Path("/system/indexer/overview")
public class IndexerOverviewResource extends RestResource {

    private final DeflectorResource deflectorResource;
    private final IndexSetRegistry indexSetRegistry;
    private final Cluster cluster;
    private final IndexerOverviewService indexerOverviewService;
    private final IndexRangesResource indexRangesResource;

    @Inject
    public IndexerOverviewResource(DeflectorResource deflectorResource,
                                   IndexSetRegistry indexSetRegistry,
                                   Cluster cluster,
                                   IndexerOverviewService indexerOverviewService,
                                   IndexRangesResource indexRangesResource) {
        this.deflectorResource = deflectorResource;
        this.indexSetRegistry = indexSetRegistry;
        this.cluster = cluster;
        this.indexerOverviewService = indexerOverviewService;
        this.indexRangesResource = indexRangesResource;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get overview of current indexing state, including deflector config, cluster state, index ranges & message counts.")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public IndexerOverview index() throws TooManyAliasesException {
        if (!cluster.isConnected()) {
            throw new ServiceUnavailableException("Opensearch cluster is not available, check your configuration and logs for more information.");
        }

        try {
            IndexSet indexSet = indexSetRegistry.getDefault();
            return indexerOverviewService.getIndexerOverview(indexSet, deflectorResource.deflector(indexSet.getConfig().id()), indexRangesResource.list().ranges());
        } catch (IllegalStateException e) {
            throw new NotFoundException("Default index set not found");
        }
    }

    @GET
    @Timed
    @Path("/{indexSetId}")
    @ApiOperation(value = "Get overview of current indexing state for the given index set, including deflector config, cluster state, index ranges & message counts.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexerOverview index(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) throws TooManyAliasesException {
        if (!cluster.isConnected()) {
            throw new ServiceUnavailableException("Opensearch cluster is not available, check your configuration and logs for more information.");
        }

        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);
        return indexerOverviewService.getIndexerOverview(indexSet, deflectorResource.deflector(indexSet.getConfig().id()), indexRangesResource.list().ranges());
    }
}
