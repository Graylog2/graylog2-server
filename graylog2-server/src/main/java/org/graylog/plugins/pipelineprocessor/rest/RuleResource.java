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

import javax.annotation.Nonnull;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(value = "Pipelines/Rules", description = "Rules for the pipeline message processor")
@Path("/system/pipelines/rule")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RuleResource extends RestResource implements PluginRestResource {

    private static final Logger log = LoggerFactory.getLogger(RuleResource.class);

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(RuleDao.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(RuleDao.FIELD_TITLE, SearchQueryField.create(RuleDao.FIELD_TITLE))
            .put(RuleDao.FIELD_DESCRIPTION, SearchQueryField.create(RuleDao.FIELD_DESCRIPTION))
            .build();

    private final RuleService ruleService;
    private final PipelineService pipelineService;
    private final RuleMetricsConfigService ruleMetricsConfigService;
    private final PipelineRuleParser pipelineRuleParser;
    private final FunctionRegistry functionRegistry;
    private final PaginatedRuleService paginatedRuleService;
    private final SearchQueryParser searchQueryParser;
    private final PipelineServiceHelper pipelineServiceHelper;

    @Inject
    public RuleResource(RuleService ruleService,
                        PipelineService pipelineService,
                        RuleMetricsConfigService ruleMetricsConfigService,
                        PipelineRuleParser pipelineRuleParser,
                        PaginatedRuleService paginatedRuleService,
                        FunctionRegistry functionRegistry,
                        PipelineServiceHelper pipelineServiceHelper) {
        this.ruleService = ruleService;
        this.pipelineService = pipelineService;
        this.ruleMetricsConfigService = ruleMetricsConfigService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.functionRegistry = functionRegistry;
        this.paginatedRuleService = paginatedRuleService;
        this.pipelineServiceHelper = pipelineServiceHelper;

        this.searchQueryParser = new SearchQueryParser(RuleDao.FIELD_TITLE, SEARCH_FIELD_MAPPING);
    }


    @ApiOperation(value = "Create a processing rule from source", notes = "")
    @POST
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_CREATE)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_CREATE)
    public RuleSource createFromParser(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        final Rule rule;
        try {
            rule = pipelineRuleParser.parseRule(ruleSource.id(), ruleSource.source(), false);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final RuleDao newRuleSource = RuleDao.builder()
                .title(rule.name()) // use the name from the parsed rule source.
                .description(ruleSource.description())
                .source(ruleSource.source())
                .createdAt(now)
                .modifiedAt(now)
                .build();

        final RuleDao save;
        try {
            save = ruleService.save(newRuleSource);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }

        log.debug("Created new rule {}", save);
        return RuleSource.fromDao(pipelineRuleParser, save);
    }

    @ApiOperation(value = "Parse a processing rule without saving it", notes = "")
    @POST
    @Path("/parse")
    @NoAuditEvent("only used to parse a rule, no changes made in the system")
    public RuleSource parse(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        final Rule rule;
        try {
            // be silent about parse errors here, many requests will result in invalid syntax
            rule = pipelineRuleParser.parseRule(ruleSource.id(), ruleSource.source(), true);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return RuleSource.builder()
                .title(rule.name())
                .description(ruleSource.description())
                .source(ruleSource.source())
                .createdAt(now)
                .modifiedAt(now)
                .build();
    }

    @ApiOperation(value = "Get all processing rules")
    @GET
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_READ)
    public Collection<RuleSource> getAll() {
        final Collection<RuleDao> ruleDaos = ruleService.loadAll();
        return ruleDaos.stream()
                .map(ruleDao -> RuleSource.fromDao(pipelineRuleParser, ruleDao))
                .collect(Collectors.toList());
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
                .map(dao -> RuleSource.fromDao(pipelineRuleParser, dao))
                .collect(Collectors.toList());
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
                                    .collect(Collectors.toList())
                    );
                });

        return ImmutableMap.of("used_in_pipelines", result);
    }



    @ApiOperation(value = "Get a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @GET
    public RuleSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_READ, id);
        return RuleSource.fromDao(pipelineRuleParser, ruleService.load(id));
    }

    @ApiOperation("Retrieve the named processing rules in bulk")
    @Path("/multiple")
    @POST
    @NoAuditEvent("only used to get multiple pipeline rules")
    public Collection<RuleSource> getBulk(@ApiParam("rules") BulkRuleRequest rules) {
        Collection<RuleDao> ruleDaos = ruleService.loadNamed(rules.rules());

        return ruleDaos.stream()
                .map(ruleDao -> RuleSource.fromDao(pipelineRuleParser, ruleDao))
                .filter(rule -> isPermitted(PipelineRestPermissions.PIPELINE_RULE_READ, rule.id()))
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Modify a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @PUT
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_UPDATE)
    public RuleSource update(@ApiParam(name = "id") @PathParam("id") String id,
                             @ApiParam(name = "rule", required = true) @NotNull RuleSource update) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_EDIT, id);

        final RuleDao ruleDao = ruleService.load(id);
        final Rule rule;
        try {
            rule = pipelineRuleParser.parseRule(id, update.source(), false);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final RuleDao toSave = ruleDao.toBuilder()
                .title(rule.name())
                .description(update.description())
                .source(update.source())
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .build();

        final RuleDao savedRule;
        try {
            savedRule = ruleService.save(toSave);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }

        return RuleSource.fromDao(pipelineRuleParser, savedRule);
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
