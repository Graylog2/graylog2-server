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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.PaginatedRuleService;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineServiceHelper;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.RuleBuilderService;
import org.graylog.plugins.pipelineprocessor.simulator.RuleSimulator;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Pipelines/Rules", description = "Rules for the pipeline message processor", tags = {CLOUD_VISIBLE})
@Path("/system/pipelines/rule")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RuleResource extends RestResource implements PluginRestResource {
    private static final RateLimitedLog log = getRateLimitedLog(RuleResource.class);

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(RuleDao.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(RuleDao.FIELD_TITLE, SearchQueryField.create(RuleDao.FIELD_TITLE))
            .put(RuleDao.FIELD_DESCRIPTION, SearchQueryField.create(RuleDao.FIELD_DESCRIPTION))
            .build();

    private final RuleService ruleService;
    private final RuleSimulator ruleSimulator;
    private final PipelineService pipelineService;
    private final RuleMetricsConfigService ruleMetricsConfigService;
    private final PipelineRuleService pipelineRuleService;
    private final FunctionRegistry functionRegistry;
    private final PaginatedRuleService paginatedRuleService;
    private final SearchQueryParser searchQueryParser;
    private final PipelineServiceHelper pipelineServiceHelper;
    private final RuleBuilderService ruleBuilderService;

    @Inject
    public RuleResource(RuleService ruleService,
                        RuleSimulator ruleSimulator, PipelineService pipelineService,
                        RuleMetricsConfigService ruleMetricsConfigService,
                        PipelineRuleService pipelineRuleService,
                        PaginatedRuleService paginatedRuleService,
                        FunctionRegistry functionRegistry,
                        PipelineServiceHelper pipelineServiceHelper,
                        StreamService streamService,
                        RuleBuilderService ruleBuilderService) {
        this.ruleService = ruleService;
        this.ruleSimulator = ruleSimulator;
        this.pipelineService = pipelineService;
        this.ruleMetricsConfigService = ruleMetricsConfigService;
        this.pipelineRuleService = pipelineRuleService;
        this.functionRegistry = functionRegistry;
        this.paginatedRuleService = paginatedRuleService;
        this.pipelineServiceHelper = pipelineServiceHelper;
        this.ruleBuilderService = ruleBuilderService;

        this.searchQueryParser = new SearchQueryParser(RuleDao.FIELD_TITLE, SEARCH_FIELD_MAPPING);
    }


    @ApiOperation(value = "Create a processing rule from source", notes = "")
    @POST
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_CREATE)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_CREATE)
    public RuleSource createFromParser(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        final Rule rule = pipelineRuleService.parseRuleOrThrow(ruleSource.id(), ruleSource.source(), false);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final RuleDao newRuleSource = RuleDao.builder()
                .title(rule.name()) // use the name from the parsed rule source.
                .description(ruleSource.description())
                .source(ruleSource.source()
                )
                .createdAt(now)
                .modifiedAt(now)
                .ruleBuilder(ruleSource.ruleBuilder())
                .simulatorMessage(ruleSource.simulatorMessage())
                .build();

        final RuleDao save;
        try {
            save = ruleService.save(newRuleSource);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }

        log.debug("Created new rule {}", save);
        return pipelineRuleService.createRuleSourceFromRuleDao(save);
    }

    @ApiOperation(value = "Parse a processing rule without saving it", notes = "")
    @POST
    @Path("/parse")
    @NoAuditEvent("only used to parse a rule, no changes made in the system")
    public RuleSource parse(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        final Rule rule = pipelineRuleService.parseRuleOrThrow(ruleSource.id(), ruleSource.source(), true);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return RuleSource.builder()
                .title(rule.name())
                .description(ruleSource.description())
                .source(ruleSource.source())
                .createdAt(now)
                .modifiedAt(now)
                .build();
    }

    @ApiOperation(value = "Simulate a single processing rule")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/simulate")
    @NoAuditEvent("only used to test a rule, no changes made in the system")
    public Message simulate(
            @ApiParam(name = "request", required = true) @NotNull SimulateRuleRequest request
    ) {
        final Rule rule = pipelineRuleService.parseRuleOrThrow(request.ruleSource().id(), request.ruleSource().source(), true);
        Message message = ruleSimulator.createMessage(request.message());
        return ruleSimulator.simulate(rule, message);
    }

    @ApiOperation(value = "Get all processing rules")
    @GET
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_READ)
    public Collection<RuleSource> getAll() {
        final Collection<RuleDao> ruleDaos = ruleService.loadAll();
        return ruleDaos.stream()
                .map(pipelineRuleService::createRuleSourceFromRuleDao)
                .toList();
    }

    @GET
    @Path("/paginated")
    @ApiOperation(value = "Get a paginated list of pipeline rules")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_READ)
    public PaginatedResponse<RuleSource> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                 @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                 @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                 @ApiParam(name = "sort",
                                                           value = "The field to sort the result on",
                                                           required = true,
                                                           allowableValues = "title,description,id")
                                                 @DefaultValue(RuleDao.FIELD_TITLE) @QueryParam("sort") String sort,
                                                 @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                 @DefaultValue("asc") @QueryParam("order") String order) {
        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }

        final PaginatedList<RuleDao> result = paginatedRuleService
                .findPaginated(searchQuery, page, perPage, sort, order);
        final List<RuleSource> ruleSourceList = result.stream()
                .map(pipelineRuleService::createRuleSourceFromRuleDao)
                .toList();
        final PaginatedList<RuleSource> rules = new PaginatedList<>(ruleSourceList,
                result.pagination().total(), result.pagination().page(), result.pagination().perPage());
        return PaginatedResponse.create("rules", rules,
                prepareContextForPaginatedResponse(result.delegate()));
    }

    @VisibleForTesting
    @Nonnull
    Map<String, Object> prepareContextForPaginatedResponse(@Nonnull List<RuleDao> rules) {
        final Map<String, RuleDao> ruleTitleMap = rules
                .stream()
                .collect(Collectors.toMap(RuleDao::title, dao -> dao));

        final Map<String, List<PipelineCompactSource>> result = new HashMap<>();
        rules.forEach(r -> result.put(r.id(), new ArrayList<>()));

        pipelineServiceHelper.groupByRuleName(
                        pipelineService::loadAll, ruleTitleMap.keySet())
                .forEach((ruleTitle, pipelineDaos) -> {
                    result.put(
                            ruleTitleMap.get(ruleTitle).id(),
                            pipelineDaos.stream()
                                    .map(dao -> PipelineCompactSource.builder()
                                            .id(dao.id())
                                            .title(dao.title())
                                            .build())
                                    .toList()
                    );
                });

        return Map.of("used_in_pipelines", result);
    }


    @ApiOperation(value = "Get a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @GET
    public RuleSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_READ, id);
        return pipelineRuleService.createRuleSourceFromRuleDao(ruleService.load(id));
    }

    @ApiOperation("Retrieve the named processing rules in bulk")
    @Path("/multiple")
    @POST
    @NoAuditEvent("only used to get multiple pipeline rules")
    public Collection<RuleSource> getBulk(@ApiParam("rules") BulkRuleRequest rules) {
        Collection<RuleDao> ruleDaos = ruleService.loadNamed(rules.rules());

        return ruleDaos.stream()
                .map(pipelineRuleService::createRuleSourceFromRuleDao)
                .filter(rule -> isPermitted(PipelineRestPermissions.PIPELINE_RULE_READ, rule.id()))
                .toList();
    }

    @ApiOperation(value = "Modify a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @PUT
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_UPDATE)
    public RuleSource update(@ApiParam(name = "id") @PathParam("id") String id,
                             @ApiParam(name = "rule", required = true) @NotNull RuleSource update) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_EDIT, id);

        final RuleDao ruleDao = ruleService.load(id);
        final Rule rule = pipelineRuleService.parseRuleOrThrow(id, update.source(), false);
        final RuleDao toSave = ruleDao.toBuilder()
                .title(rule.name())
                .description(update.description())
                .source(update.source())
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .ruleBuilder(update.ruleBuilder())
                .simulatorMessage(update.simulatorMessage())
                .build();

        final RuleDao savedRule;
        try {
            savedRule = ruleService.save(toSave);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }

        return pipelineRuleService.createRuleSourceFromRuleDao(savedRule);
    }

    @ApiOperation(value = "Delete a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @DELETE
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_DELETE)
    public void delete(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_DELETE, id);
        ruleService.load(id);
        ruleService.delete(id);
    }

    @ApiOperation("Get function descriptors")
    @Path("/functions")
    @GET
    public Collection<Object> functionDescriptors() {
        return functionRegistry.all().stream()
                .map(Function::descriptor)
                .collect(Collectors.toList());
    }

    @ApiOperation("Get function descriptors for rule builder")
    @Path("/rulebuilder/functions")
    @GET
    public Collection<Object> rulebuilderFunctions() {
        return functionRegistry.all().stream()
                .filter(f -> f.descriptor().ruleBuilderEnabled())
                .map(Function::descriptor)
                .collect(Collectors.toList());
    }

    @ApiOperation("Get condition descriptors for ruleBuilder")
    @Path("/rulebuilder/conditions")
    @GET
    public Collection<Object> rulebuilderConditions() {
        return functionRegistry.all().stream()
                .filter(f -> f.descriptor().ruleBuilderEnabled() && f.descriptor().returnType().equals(Boolean.class))
                .map(Function::descriptor)
                .collect(Collectors.toList());
    }

    @ApiOperation("Get rule metrics configuration")
    @Path("/config/metrics")
    @GET
    public RuleMetricsConfigDto metricsConfig() {
        return ruleMetricsConfigService.get();
    }

    @ApiOperation("Update rule metrics configuration")
    @Path("/config/metrics")
    @PUT
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_METRICS_UPDATE)
    public RuleMetricsConfigDto updateMetricsConfig(RuleMetricsConfigDto config) {
        return ruleMetricsConfigService.save(config);
    }
}
