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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.PaginatedPipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Pipelines/Pipelines", description = "Pipelines for the pipeline message processor", tags = {CLOUD_VISIBLE})
@Path("/system/pipelines/pipeline")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PipelineResource extends RestResource implements PluginRestResource {
    private static final Logger log = LoggerFactory.getLogger(PipelineResource.class);

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(PipelineDao.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(PipelineDao.FIELD_TITLE, SearchQueryField.create(PipelineDao.FIELD_TITLE))
            .put(PipelineDao.FIELD_DESCRIPTION, SearchQueryField.create(PipelineDao.FIELD_DESCRIPTION))
            .build();

    private final SearchQueryParser searchQueryParser;
    private final PaginatedPipelineService paginatedPipelineService;

    private final PipelineService pipelineService;
    private final PipelineRuleParser pipelineRuleParser;

    @Inject
    public PipelineResource(PipelineService pipelineService,
                            PaginatedPipelineService paginatedPipelineService,
                            PipelineRuleParser pipelineRuleParser) {
        this.pipelineService = pipelineService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.paginatedPipelineService = paginatedPipelineService;
        this.searchQueryParser = new SearchQueryParser(PipelineDao.FIELD_TITLE, SEARCH_FIELD_MAPPING);
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
                                stage.match(),
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

    @GET
    @Path("/paginated")
    @ApiOperation(value = "Get a paginated list of pipelines")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<PipelineSource> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                     @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                     @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                     @ApiParam(name = "sort",
                                             value = "The field to sort the result on",
                                             required = true,
                                             allowableValues = "title,description,id")
                                     @DefaultValue(PipelineDao.FIELD_TITLE) @QueryParam("sort") String sort,
                                     @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                     @DefaultValue("asc") @QueryParam("order") String order) {

        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }

        Predicate<PipelineDao> filter = dao -> isPermitted(PipelineRestPermissions.PIPELINE_READ, dao.id());

        final PaginatedList<PipelineDao> result = paginatedPipelineService
                .findPaginated(searchQuery, filter, page, perPage, sort, order);
        final List<PipelineSource> pipelineList = result.stream()
                .map(dao -> PipelineSource.fromDao(pipelineRuleParser, dao))
                .collect(Collectors.toList());
        final PaginatedList<PipelineSource> pipelines = new PaginatedList<>(pipelineList,
                result.pagination().total(), result.pagination().page(), result.pagination().perPage());
        return PaginatedResponse.create("pipelines", pipelines);
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
