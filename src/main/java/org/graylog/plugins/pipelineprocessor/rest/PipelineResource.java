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

@Api(value = "Pipelines", description = "Pipelines for the pipeline message processor")
@Path("/system/pipelines")
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
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    @Path("/pipeline")
    public PipelineSource createFromParser(@ApiParam(name = "pipeline", required = true) @NotNull String pipelineSource) throws ParseException {
        try {
            pipelineRuleParser.parsePipelines(pipelineSource);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final PipelineSource newPipelineSource = PipelineSource.builder()
                .source(pipelineSource)
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
        final PipelineSource save = pipelineSourceService.save(newPipelineSource);
        clusterBus.post(PipelinesChangedEvent.updatedPipelineId(save.id()));
        log.info("Created new pipeline {}", save);
        return save;
    }

    @ApiOperation(value = "Get all processing pipelines")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("/pipeline")
    public Collection<PipelineSource> getAll() {
        return pipelineSourceService.loadAll();
    }

    @ApiOperation(value = "Get a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pipeline/{id}")
    @GET
    public PipelineSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        return pipelineSourceService.load(id);
    }

    @ApiOperation(value = "Modify a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pipeline/{id}")
    @PUT
    public PipelineSource update(@ApiParam(name = "id") @PathParam("id") String id,
                             @ApiParam(name = "pipeline", required = true) @NotNull PipelineSource update) throws NotFoundException {
        final PipelineSource pipelineSource = pipelineSourceService.load(id);
        try {
            pipelineRuleParser.parsePipelines(update.source());
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pipeline/{id}")
    @DELETE
    public void delete(@ApiParam(name = "id") @PathParam("id") String id) {
        pipelineSourceService.delete(id);
        clusterBus.post(PipelinesChangedEvent.deletedPipelineId(id));
    }

}
