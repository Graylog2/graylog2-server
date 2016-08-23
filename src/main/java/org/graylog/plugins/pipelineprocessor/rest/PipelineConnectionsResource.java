/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "Pipelines/Connections", description = "Stream connections of processing pipelines")
@Path("/system/pipelines/connections")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PipelineConnectionsResource extends RestResource implements PluginRestResource {

    private final PipelineStreamConnectionsService connectionsService;
    private final PipelineService pipelineService;
    private final StreamService streamService;
    private final EventBus clusterBus;

    @Inject
    public PipelineConnectionsResource(PipelineStreamConnectionsService connectionsService,
                                       PipelineService pipelineService,
                                       StreamService streamService,
                                       ClusterEventBus clusterBus) {
        this.connectionsService = connectionsService;
        this.pipelineService = pipelineService;
        this.streamService = streamService;
        this.clusterBus = clusterBus;
    }

    @ApiOperation(value = "Connect processing pipelines to a stream", notes = "")
    @POST
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CONNECTION_EDIT)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_CONNECTION_UPDATE)
    public PipelineConnections connectPipelines(@ApiParam(name = "Json body", required = true) @NotNull PipelineConnections connection) throws NotFoundException {
        final String streamId = connection.streamId();
        // the default stream doesn't exist as an entity
        if (!streamId.equalsIgnoreCase("default")) {
            checkPermission(RestPermissions.STREAMS_READ, streamId);
            streamService.load(streamId);
        }
        // verify the pipelines exist
        for (String s : connection.pipelineIds()) {
            checkPermission(PipelineRestPermissions.PIPELINE_READ, s);
            pipelineService.load(s);
        }
        final PipelineConnections save = connectionsService.save(connection);
        clusterBus.post(PipelineConnectionsChangedEvent.create(save.streamId(), save.pipelineIds()));
        return save;
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


        // to simplify clients, we always return the default stream, until we have it as a true entity
        if (!filteredConnections.stream().anyMatch(pc -> pc.streamId().equals("default"))) {
            filteredConnections.add(PipelineConnections.create(null, "default", Collections.emptySet()));
        }
        return filteredConnections;
    }

}
