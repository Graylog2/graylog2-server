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
package org.graylog.plugins.pipelineprocessor.rulebuilder.rest;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog.plugins.pipelineprocessor.rest.RuleResource;
import org.graylog.plugins.pipelineprocessor.rest.RuleSource;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.RuleBuilderService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Pipelines/Rulebuilder", description = "Rules for the pipeline message processor generated using the rulebuilder", tags = {CLOUD_VISIBLE})
@Path("/system/pipelines/rulebuilder")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RuleBuilderResource extends RestResource implements PluginRestResource {
    private static final RateLimitedLog log = getRateLimitedLog(RuleBuilderResource.class);
    private final RuleBuilderRegistry ruleBuilderRegistry;
    private final RuleResource ruleResource;
    private final RuleBuilderService ruleBuilderParser;

    @Inject
    public RuleBuilderResource(RuleBuilderRegistry ruleBuilderRegistry,
                               RuleResource ruleResource,
                               RuleBuilderService ruleBuilderParser) {
        this.ruleBuilderRegistry = ruleBuilderRegistry;
        this.ruleResource = ruleResource;
        this.ruleBuilderParser = ruleBuilderParser;
    }


    @ApiOperation(value = "Create a processing rule from rule builder", notes = "")
    @POST
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_CREATE)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_CREATE)
    public RuleBuilderDto createFromBuilder(@ApiParam(name = "rule", required = true) @NotNull RuleBuilderDto ruleBuilderDto) {
        RuleSource ruleSource = toRuleSource(ruleBuilderDto);
        final RuleSource stored = ruleResource.createFromParser(ruleSource);
        return ruleBuilderDto.toBuilder().ruleId(stored.id()).build();
    }

    @ApiOperation(value = "Create a processing rule from rule builder", notes = "")
    @Path("/{id}")
    @PUT
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_CREATE)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_CREATE)
    public RuleBuilderDto updateFromBuilder(@ApiParam(name = "id") @PathParam("id") String id,
                                            @ApiParam(name = "rule", required = true) @NotNull RuleBuilderDto ruleBuilderDto) throws NotFoundException {
        RuleSource ruleSource = toRuleSource(ruleBuilderDto);
        final RuleSource stored = ruleResource.update(id, ruleSource);
        return ruleBuilderDto.toBuilder().ruleId(stored.id()).build();
    }

    @ApiOperation("Get action descriptors for rule builder")
    @Path("/actions")
    @GET
    public Collection<Object> actions() {
        return ruleBuilderRegistry.actions()
                .values().stream()
                .map(RuleFragment::descriptor)
                .collect(Collectors.toList());
    }

    @ApiOperation("Get condition descriptors for ruleBuilder")
    @Path("/conditions")
    @GET
    public Collection<Object> conditions() {
        return ruleBuilderRegistry.conditions()
                .values().stream()
                .map(RuleFragment::descriptor)
                .collect(Collectors.toList());
    }


    private RuleSource toRuleSource(RuleBuilderDto ruleBuilderDto) {
        return RuleSource.builder()
                .title(ruleBuilderDto.title())
                .description(ruleBuilderDto.description())
                .ruleBuilder(ruleBuilderDto.ruleBuilder())
                .source(ruleBuilderParser.generateRuleSource(ruleBuilderDto.title(), ruleBuilderDto.ruleBuilder(), true))
                .build();
    }

}
