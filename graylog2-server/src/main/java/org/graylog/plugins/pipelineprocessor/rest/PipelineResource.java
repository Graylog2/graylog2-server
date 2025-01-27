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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.PaginatedPipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.shared.utilities.StringUtils.f;

@Api(value = "Pipelines/Pipelines", description = "Pipelines for the pipeline message processor", tags = {CLOUD_VISIBLE})
@Path("/system/pipelines/pipeline")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PipelineResource extends RestResource implements PluginRestResource {
    private static final RateLimitedLog log = getRateLimitedLog(PipelineResource.class);

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(PipelineDao.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(PipelineDao.FIELD_TITLE, SearchQueryField.create(PipelineDao.FIELD_TITLE))
            .put(PipelineDao.FIELD_DESCRIPTION, SearchQueryField.create(PipelineDao.FIELD_DESCRIPTION))
            .build();
    public static final String GL_INPUT_ROUTING_PIPELINE = "All Messages Routing";

    private final SearchQueryParser searchQueryParser;
    private final PaginatedPipelineService paginatedPipelineService;

    private final PipelineService pipelineService;
    private final PipelineRuleParser pipelineRuleParser;
    private final PipelineStreamConnectionsService connectionsService;
    private final RuleService ruleService;
    private final StreamService streamService;

    @Inject
    public PipelineResource(PipelineService pipelineService,
                            PaginatedPipelineService paginatedPipelineService,
                            PipelineRuleParser pipelineRuleParser,
                            PipelineStreamConnectionsService connectionsService,
                            RuleService ruleService,
                            StreamService streamService) {
        this.pipelineService = pipelineService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.paginatedPipelineService = paginatedPipelineService;
        this.searchQueryParser = new SearchQueryParser(PipelineDao.FIELD_TITLE, SEARCH_FIELD_MAPPING);
        this.connectionsService = connectionsService;
        this.ruleService = ruleService;
        this.streamService = streamService;
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

        final PipelineDao save;
        try {
            save = pipelineService.save(pipelineDao);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }

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

        final PipelineDao savedPipeline;
        try {
            savedPipeline = pipelineService.save(toSave);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }

        return PipelineSource.fromDao(pipelineRuleParser, savedPipeline);
    }

    public record RoutingRequest(
            @JsonProperty(value = "input_id", required = true) String inputId,
            @JsonProperty(value = "stream_id", required = true) String streamId,
            @Nullable @JsonProperty(value = "remove_from_default") Boolean removeFromDefault
    ) {}

    public record RoutingResponse(@JsonProperty(value = "rule_id") String ruleId) {}

    @ApiOperation(value = "Add a stream routing rule to the default routing pipeline.")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/routing")
    @PUT
    @AuditEvent(type = PipelineProcessorAuditEventTypes.PIPELINE_UPDATE)
    public RoutingResponse routing(@ApiParam(name = "body", required = true) @NotNull RoutingRequest request) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, request.streamId());
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_CREATE);

        Stream stream;
        try {
            stream = streamService.load(request.streamId());
        } catch (NotFoundException e) {
            throw new NotFoundException(f("Unable to load stream %s", request.streamId()), e);
        }

        boolean removeFromDefault = true;
        if (request.removeFromDefault() == null) {
            removeFromDefault = stream.getRemoveMatchesFromDefaultStream();
        }

        RuleDao ruleDao = createRoutingRule(request, removeFromDefault, stream.getTitle());
        PipelineDao pipelineDao;
        try {
            pipelineDao = pipelineService.loadByName(GL_INPUT_ROUTING_PIPELINE);
            ensurePipelineConnection(pipelineDao.id(), DEFAULT_STREAM_ID);
        } catch (NotFoundException e) {
            // Create pipeline with first rule
            createRoutingPipeline(ruleDao);
            return new RoutingResponse(ruleDao.id());
        }

        // Add rule to existing pipeline
        PipelineSource pipelineSource = PipelineSource.fromDao(pipelineRuleParser, pipelineDao);
        final List<String> rules0 = pipelineSource.stages().get(0).rules();
        if (rules0.stream().filter(ruleRef -> ruleRef.equals(ruleDao.title())).findFirst().isEmpty()) {
            rules0.add(ruleDao.title());
            pipelineSource = pipelineSource.toBuilder()
                    .source(createPipelineString(pipelineSource))
                    .build();
            update(pipelineDao.id(), pipelineSource);
        } else {
            log.info(f("Routing for input <%s> already exists - skipping", request.inputId()));
        }

        return new RoutingResponse(ruleDao.id());
    }

    private PipelineSource createRoutingPipeline(RuleDao ruleDao) {
        List<StageSource> stages = java.util.List.of(StageSource.create(
                0, Stage.Match.EITHER, java.util.List.of(ruleDao.title())));
        final PipelineSource pipelineSource = PipelineSource.builder()
                .title(GL_INPUT_ROUTING_PIPELINE)
                .description("GL generated pipeline")
                .source("pipeline \"" + GL_INPUT_ROUTING_PIPELINE + "\"\nstage 0 match either\nrule \"" + ruleDao.title() + "\"\nend")
                .stages(stages)
                .build();
        final PipelineSource parsedSource = createFromParser(pipelineSource);
        ensurePipelineConnection(parsedSource.id(), DEFAULT_STREAM_ID);
        return parsedSource;
    }

    private void ensurePipelineConnection(String pipelineId, String streamId) {
        PipelineConnections pipelineConnections;
        try {
            pipelineConnections = connectionsService.load(streamId);
            if (pipelineConnections.pipelineIds().stream()
                    .anyMatch(id -> id.equals(pipelineId))) {
                return;
            }
        } catch (NotFoundException e) {
            pipelineConnections = PipelineConnections.create(null, streamId, new HashSet<>());
        }
        pipelineConnections.pipelineIds().add(pipelineId);
        connectionsService.save(pipelineConnections);
    }

    private RuleDao createRoutingRule(RoutingRequest request, boolean removeFromDefault, String streamName) {
        String ruleName = "route_" + request.inputId() + "_to_" + streamName;
        final Optional<RuleDao> ruleDaoOpt = ruleService.findByName(ruleName);
        if (ruleDaoOpt.isPresent()) {
            log.info(f("Routing rule %s already exists - skipping", ruleName));
            return ruleDaoOpt.get();
        }

        String ruleSource =
                "rule \"" + ruleName + "\"\n"
                        + "when has_field(\"gl2_source_input\") AND to_string($message.gl2_source_input)==\"" + request.inputId() + "\"\n"
                        + "then\n"
                        + "route_to_stream(id:\"" + request.streamId() + "\""
                        + ", remove_from_default: " + removeFromDefault
                        + ");\nend\n";

        RuleDao ruleDao = RuleDao.builder()
                .title(ruleName)
                .description("Input setup wizard routing rule")
                .source(ruleSource)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();
        return ruleService.save(ruleDao);
    }

    @VisibleForTesting
    public static String createPipelineString(PipelineSource pipelineSource) {
        StringBuilder result = new StringBuilder("pipeline \"" + pipelineSource.title() + "\"\n");
        for (int stageNr = 0; stageNr < pipelineSource.stages().size(); stageNr++) {
            StageSource currStage = pipelineSource.stages().get(stageNr);
            result.append("stage ").append(stageNr).append(" match ").append(currStage.match()).append('\n');
            for (String rule : currStage.rules()) {
                result.append("rule \"").append(rule).append("\"\n");
            }
        }
        result.append("end");

        return result.toString();
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
