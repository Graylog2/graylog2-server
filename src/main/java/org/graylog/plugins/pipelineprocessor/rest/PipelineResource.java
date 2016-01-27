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

import com.google.common.eventbus.EventBus;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.graylog.plugins.pipelineprocessor.db.PipelineSourceService;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Api(value = "Pipeline Pipelines", description = "Pipelines for the pipeline message processor")
@Path("/system/pipelines")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PipelineResource extends RestResource implements PluginRestResource {

    private static final Logger log = LoggerFactory.getLogger(PipelineResource.class);

    private final PipelineSourceService pipelineSourceService;
    private final PipelineRuleParser pipelineRuleParser;
    private final EventBus clusterBus;

    @Inject
    public PipelineResource(PipelineSourceService pipelineSourceService,
                        PipelineRuleParser pipelineRuleParser,
                        @ClusterEventBus EventBus clusterBus) {
        this.pipelineSourceService = pipelineSourceService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.clusterBus = clusterBus;
    }


    @ApiOperation(value = "Create a processing pipeline from source", notes = "")
    @POST
    @Path("/pipeline")
    public PipelineSource createFromParser(@ApiParam(name = "pipeline", required = true) @NotNull PipelineSource pipelineSource) throws ParseException {
        try {
            pipelineRuleParser.parsePipeline(pipelineSource.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final PipelineSource newPipelineSource = PipelineSource.builder()
                .title(pipelineSource.title())
                .description(pipelineSource.description())
                .source(pipelineSource.source())
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
        final PipelineSource save = pipelineSourceService.save(newPipelineSource);
        clusterBus.post(PipelinesChangedEvent.updatedPipelineId(save.id()));
        log.info("Created new pipeline {}", save);
        return save;
    }

    @ApiOperation(value = "Parse a processing pipeline without saving it", notes = "")
    @POST
    @Path("/pipeline/parse")
    public PipelineSource parse(@ApiParam(name = "pipeline", required = true) @NotNull PipelineSource pipelineSource) throws ParseException {
        try {
            pipelineRuleParser.parsePipeline(pipelineSource.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        return PipelineSource.builder()
                .title(pipelineSource.title())
                .description(pipelineSource.description())
                .source(pipelineSource.source())
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
    }

    @ApiOperation(value = "Get all processing pipelines")
    @GET
    @Path("/pipeline")
    public Collection<PipelineSource> getAll() {
        return  pipelineSourceService.loadAll();
    }

    @ApiOperation(value = "Get a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Path("/pipeline/{id}")
    @GET
    public PipelineSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        return pipelineSourceService.load(id);
    }

    @ApiOperation(value = "Modify a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Path("/pipeline/{id}")
    @PUT
    public PipelineSource update(@ApiParam(name = "id") @PathParam("id") String id,
                             @ApiParam(name = "pipeline", required = true) @NotNull PipelineSource update) throws NotFoundException {
        final PipelineSource pipelineSource = pipelineSourceService.load(id);
        try {
            pipelineRuleParser.parsePipeline(update.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final PipelineSource toSave = pipelineSource.toBuilder()
                .source(update.source())
                .modifiedAt(DateTime.now())
                .build();
        final PipelineSource savedPipeline = pipelineSourceService.save(toSave);
        clusterBus.post(PipelinesChangedEvent.updatedPipelineId(savedPipeline.id()));

        return savedPipeline;
    }

    @ApiOperation(value = "Delete a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Path("/pipeline/{id}")
    @DELETE
    public void delete(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        pipelineSourceService.load(id);
        pipelineSourceService.delete(id);
        clusterBus.post(PipelinesChangedEvent.deletedPipelineId(id));
    }

}
