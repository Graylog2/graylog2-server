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
import com.swrve.ratelimitedlogger.RateLimitedLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Pipelines/Connections", description = "Stream connections of processing pipelines", tags = {CLOUD_VISIBLE})
@Path("/system/pipelines/connections")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PipelineConnectionsResource extends RestResource implements PluginRestResource {
    private static final RateLimitedLog LOG = createDefaultRateLimitedLog(PipelineConnectionsResource.class);

    private final PipelineStreamConnectionsService connectionsService;
    private final PipelineService pipelineService;
    private final StreamService streamService;
    private final EntityScopeService entityScopeService;

    @Inject
    public PipelineConnectionsResource(PipelineStreamConnectionsService connectionsService,
                                       PipelineService pipelineService,
                                       StreamService streamService,
                                       EntityScopeService entityScopeService) {
        this.connectionsService = connectionsService;
        this.pipelineService = pipelineService;
        this.streamService = streamService;
        this.entityScopeService = entityScopeService;
    }

    @ApiOperation(value = "Connect processing pipelines to a stream")
    @POST
    @Path("/to_stream")
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CONNECTION_EDIT)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_CONNECTION_UPDATE)
    public PipelineConnections connectPipelines(@ApiParam(name = "Json body", required = true) @NotNull PipelineConnections connection) throws NotFoundException {
        final String streamId = connection.streamId();

        // verify the stream exists and is readable
        checkPermission(RestPermissions.STREAMS_READ, streamId);
        final Stream stream = streamService.load(streamId);
        checkNotEditable(stream, "Cannot connect pipeline to non editable stream");

        // verify the pipelines exist
        for (String s : connection.pipelineIds()) {
            checkPermission(PipelineRestPermissions.PIPELINE_READ, s);
            checkScope(pipelineService.load(s));
        }
        return connectionsService.save(connection);
    }

    @ApiOperation(value = "Connect streams to a processing pipeline")
    @POST
    @Path("/to_pipeline")
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CONNECTION_EDIT)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_CONNECTION_UPDATE)
    public Set<PipelineConnections> connectStreams(@ApiParam(name = "Json body", required = true) @NotNull PipelineReverseConnections connection) throws NotFoundException {
        final String pipelineId = connection.pipelineId();
        final Set<PipelineConnections> updatedConnections = Sets.newHashSet();

        // verify the pipeline exists and is editable
        checkPermission(PipelineRestPermissions.PIPELINE_READ, pipelineId);
        checkScope(pipelineService.load(pipelineId));

        // get all connections where the pipeline was present
        final Set<PipelineConnections> pipelineConnections = connectionsService.loadAll().stream()
                .filter(p -> p.pipelineIds().contains(pipelineId))
                .collect(Collectors.toSet());

        // verify the streams exist and the user has permission to read them
        final Set<Stream> connectedStreams = streamService.loadByIds(connection.streamIds());
        connectedStreams.forEach(stream -> {
            checkPermission(RestPermissions.STREAMS_READ, stream.getId());
            checkNotEditable(stream, "Cannot connect pipeline to non editable stream");
        });

        // remove deleted pipeline connections
        for (PipelineConnections pipelineConnection : pipelineConnections) {
            if (!connection.streamIds().contains(pipelineConnection.streamId())) {
                final Set<String> pipelines = pipelineConnection.pipelineIds();
                pipelines.remove(connection.pipelineId());

                updatedConnections.add(pipelineConnection);
                connectionsService.save(pipelineConnection);
                LOG.debug("Deleted stream {} connection with pipeline {}", pipelineConnection.streamId(), pipelineId);
            }
        }

        // update pipeline connections
        for (Stream stream : connectedStreams) {
            final String streamId = stream.getId();
            PipelineConnections updatedConnection;
            try {
                updatedConnection = connectionsService.load(streamId);
            } catch (NotFoundException e) {
                updatedConnection = PipelineConnections.create(null, streamId, Sets.newHashSet());
            }

            final Set<String> pipelines = updatedConnection.pipelineIds();
            pipelines.add(pipelineId);

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

    private void checkNotEditable(Stream stream, String message) {
        if (!stream.isEditable()) {
            throw new BadRequestException(message);
        }
    }

    private void checkScope(PipelineDao pipelineDao) {
        if (!entityScopeService.isMutable(pipelineDao)) {
            throw new BadRequestException("Cannot modify connections for immutable pipeline");
        }
    }
}
