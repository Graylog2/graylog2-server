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

import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@Api(value = "Pipelines/Pipelines", description = "Pipelines for the pipeline message processor")
@Path("/system/pipelines/pipeline")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PipelineResource extends RestResource implements PluginRestResource {
    private static final Logger log = LoggerFactory.getLogger(PipelineResource.class);

    private final PipelineService pipelineService;
    private final PipelineRuleParser pipelineRuleParser;

    @Inject
    public PipelineResource(PipelineService pipelineService,
                            PipelineRuleParser pipelineRuleParser) {
        this.pipelineService = pipelineService;
        this.pipelineRuleParser = pipelineRuleParser;
    }

    @ApiOperation(value = "Create a processing pipeline from source")
    @POST
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_CREATE)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_CREATE)
    public PipelineSource createFromParser(@ApiParam(name = "pipeline", required = true) @NotNull PipelineSource pipelineSource) throws ParseException {
        final Pipeline pipeline;
        try {
            pipeline = pipelineRuleParser.parsePipeline(pipelineSource.id(), pipelineSource.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final PipelineDao pipelineDao = PipelineDao.builder()
                .title(pipeline.name())
                .description(pipelineSource.description())
                .source(pipelineSource.source())
                .createdAt(now)
                .modifiedAt(now)
                .build();
        final PipelineDao save = pipelineService.save(pipelineDao);

        log.debug("Created new pipeline {}", save);
        return PipelineSource.fromDao(pipelineRuleParser, save);
    }

    @ApiOperation(value = "Parse a processing pipeline without saving it")
    @POST
    @Path("/parse")
    @NoAuditEvent("only used to parse a pipeline, no changes made in the system")
    public PipelineSource parse(@ApiParam(name = "pipeline", required = true) @NotNull PipelineSource pipelineSource) throws ParseException {
        final Pipeline pipeline;
        try {
            pipeline = pipelineRuleParser.parsePipeline(pipelineSource.id(), pipelineSource.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return PipelineSource.builder()
                .title(pipeline.name())
                .description(pipelineSource.description())
                .source(pipelineSource.source())
                .stages(pipeline.stages().stream()
                        .map(stage -> StageSource.create(
                                stage.stage(),
                                stage.matchAll(),
                                stage.ruleReferences()))
                        .collect(Collectors.toList()))
                .createdAt(now)
                .modifiedAt(now)
                .build();
    }

    @ApiOperation(value = "Get all processing pipelines")
    @GET
    public Collection<PipelineSource> getAll() {
        final Collection<PipelineDao> daos = pipelineService.loadAll();
        final ArrayList<PipelineSource> results = Lists.newArrayList();
        for (PipelineDao dao : daos) {
            if (isPermitted(PipelineRestPermissions.PIPELINE_READ, dao.id())) {
                results.add(PipelineSource.fromDao(pipelineRuleParser, dao));
            }
        }

        return results;
    }

    @ApiOperation(value = "Get a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @GET
    public PipelineSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_READ, id);
        final PipelineDao dao = pipelineService.load(id);
        return PipelineSource.fromDao(pipelineRuleParser, dao);
    }

    @ApiOperation(value = "Modify a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @PUT
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_UPDATE)
    public PipelineSource update(@ApiParam(name = "id") @PathParam("id") String id,
                                 @ApiParam(name = "pipeline", required = true) @NotNull PipelineSource update) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_EDIT, id);

        final PipelineDao dao = pipelineService.load(id);
        final Pipeline pipeline;
        try {
            pipeline = pipelineRuleParser.parsePipeline(update.id(), update.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final PipelineDao toSave = dao.toBuilder()
                .title(pipeline.name())
                .description(update.description())
                .source(update.source())
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        final PipelineDao savedPipeline = pipelineService.save(toSave);

        return PipelineSource.fromDao(pipelineRuleParser, savedPipeline);
    }

    @ApiOperation(value = "Delete a processing pipeline", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @DELETE
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_DELETE)
    public void delete(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_DELETE, id);
        pipelineService.load(id);
        pipelineService.delete(id);
    }
}
