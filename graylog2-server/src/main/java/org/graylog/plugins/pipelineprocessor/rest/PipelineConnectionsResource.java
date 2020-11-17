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
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "Pipelines/Connections", description = "Stream connections of processing pipelines")
@Path("/system/pipelines/connections")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PipelineConnectionsResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineConnectionsResource.class);

    private final PipelineStreamConnectionsService connectionsService;
    private final PipelineService pipelineService;
    private final StreamService streamService;

    @Inject
    public PipelineConnectionsResource(PipelineStreamConnectionsService connectionsService,
                                       PipelineService pipelineService,
                                       StreamService streamService) {
        this.connectionsService = connectionsService;
        this.pipelineService = pipelineService;
        this.streamService = streamService;
    }

    @ApiOperation(value = "Connect processing pipelines to a stream", notes = "")
    @POST
    @Path("/to_stream")
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CONNECTION_EDIT)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_CONNECTION_UPDATE)
    public PipelineConnections connectPipelines(@ApiParam(name = "Json body", required = true) @NotNull PipelineConnections connection) throws NotFoundException {
        final String streamId = connection.streamId();
        // verify the stream exists
        checkPermission(RestPermissions.STREAMS_READ, streamId);
        streamService.load(streamId);

        // verify the pipelines exist
        for (String s : connection.pipelineIds()) {
            checkPermission(PipelineRestPermissions.PIPELINE_READ, s);
            pipelineService.load(s);
        }
        return connectionsService.save(connection);
    }

    @ApiOperation(value = "Connect streams to a processing pipeline", notes = "")
    @POST
    @Path("/to_pipeline")
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CONNECTION_EDIT)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_CONNECTION_UPDATE)
    public Set<PipelineConnections> connectStreams(@ApiParam(name = "Json body", required = true) @NotNull PipelineReverseConnections connection) throws NotFoundException {
        final String pipelineId = connection.pipelineId();
        final Set<PipelineConnections> updatedConnections = Sets.newHashSet();

        // verify the pipeline exists
        checkPermission(PipelineRestPermissions.PIPELINE_READ, pipelineId);
        pipelineService.load(pipelineId);

        // get all connections where the pipeline was present
        final Set<PipelineConnections> pipelineConnections = connectionsService.loadAll().stream()
                .filter(p -> p.pipelineIds().contains(pipelineId))
                .collect(Collectors.toSet());

        // remove deleted pipeline connections
        for (PipelineConnections pipelineConnection : pipelineConnections) {
            if (!connection.streamIds().contains(pipelineConnection.streamId())) {
                final Set<String> pipelines = pipelineConnection.pipelineIds();
                pipelines.remove(connection.pipelineId());
                pipelineConnection.toBuilder().pipelineIds(pipelines).build();

                updatedConnections.add(pipelineConnection);
                connectionsService.save(pipelineConnection);
                LOG.debug("Deleted stream {} connection with pipeline {}", pipelineConnection.streamId(), pipelineId);
            }
        }

        // update pipeline connections
        for (String streamId : connection.streamIds()) {
            // verify the stream exist
            checkPermission(RestPermissions.STREAMS_READ, streamId);
            streamService.load(streamId);

            PipelineConnections updatedConnection;
            try {
                updatedConnection = connectionsService.load(streamId);
            } catch (NotFoundException e) {
                updatedConnection = PipelineConnections.create(null, streamId, Sets.newHashSet());
            }

            final Set<String> pipelines = updatedConnection.pipelineIds();
            pipelines.add(pipelineId);
            updatedConnection.toBuilder().pipelineIds(pipelines).build();

            updatedConnections.add(updatedConnection);
            connectionsService.save(updatedConnection);
            LOG.debug("Added stream {} connection with pipeline {}", streamId, pipelineId);
        }

        return updatedConnections;
    }

    @ApiOperation("Get pipeline connections for the given stream")
    @GET
    @Path("/{streamId}")
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CONNECTION_READ)
    public PipelineConnections getPipelinesForStream(@ApiParam(name = "streamId") @PathParam("streamId") String streamId) throws NotFoundException {
        // the user needs to at least be able to read the stream
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final PipelineConnections connections = connectionsService.load(streamId);
        // filter out all pipelines the user does not have enough permissions to see
        return PipelineConnections.create(
                connections.id(),
                connections.streamId(),
                connections.pipelineIds()
                        .stream()
                        .filter(id -> isPermitted(PipelineRestPermissions.PIPELINE_READ, id))
                        .collect(Collectors.toSet())
        );
    }

    @ApiOperation("Get all pipeline connections")
    @GET
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CONNECTION_READ)
    public Set<PipelineConnections> getAll() throws NotFoundException {
        final Set<PipelineConnections> pipelineConnections = connectionsService.loadAll();

        final Set<PipelineConnections> filteredConnections = Sets.newHashSetWithExpectedSize(pipelineConnections.size());
        for (PipelineConnections pc : pipelineConnections) {
            // only include the streams the user can see
            if (isPermitted(RestPermissions.STREAMS_READ, pc.streamId())) {
                // filter out all pipelines the user does not have enough permissions to see
                filteredConnections.add(PipelineConnections.create(
                        pc.id(),
                        pc.streamId(),
                        pc.pipelineIds()
                                .stream()
                                .filter(id -> isPermitted(PipelineRestPermissions.PIPELINE_READ, id))
                                .collect(Collectors.toSet()))
                );
            }
        }

        return filteredConnections;
    }

}
