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
package org.graylog2.rest.resources.streams.destinations.filters;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.RuleBuilderService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidatorService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.rest.RuleBuilderDto;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNullElse;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Stream/Destinations/Filters/Builder", description = "Stream destination filter builder", tags = {CLOUD_VISIBLE})
@Path("/streams/destinations/filters/builder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class StreamDestinationFilterBuilderResource extends RestResource {
    // We want to control the available conditions for the stream destination filters to avoid exposing functions
    // that don't make sense in the destination filter context.
    // Check "/api/system/pipelines/rulebuilder/conditions" for all available conditions.
    private static final Set<String> INCLUDED_CONDITIONS = ImmutableSet.<String>builder()
            .add("array_contains")
            .add("field_cidr")
            .add("field_contains")
            .add("field_ends_with")
            .add("field_ip")
            .add("field_not_null")
            .add("field_null")
            .add("field_starts_with")
            .add("field_url")
            .add("from_forwarder_input")
            .add("from_input")
            .add("grok_matches")
            .add("has_field")
            .add("has_field_equals")
            .add("has_field_greater_or_equal")
            .add("has_field_less_or_equal")
            .add("lookup_has_value")
            .add("lookup_string_list_contains")
            .build();

    private final RuleBuilderRegistry ruleBuilderRegistry;
    private final ValidatorService validatorService;
    private final RuleBuilderService ruleBuilderService;

    @Inject
    public StreamDestinationFilterBuilderResource(RuleBuilderRegistry ruleBuilderRegistry,
                                                  ValidatorService validatorService,
                                                  RuleBuilderService ruleBuilderService) {
        this.ruleBuilderRegistry = ruleBuilderRegistry;
        this.validatorService = validatorService;
        this.ruleBuilderService = ruleBuilderService;
    }

    @GET
    @Path("/conditions")
    @ApiOperation(value = "Get available filter rule conditions")
    @RequiresPermissions(RestPermissions.STREAM_DESTINATION_FILTERS_READ)
    public Response getConditions() {
        final var conditions = ruleBuilderRegistry.conditions()
                .values()
                .stream()
                .map(RuleFragment::descriptor)
                .filter(descriptor -> INCLUDED_CONDITIONS.contains(descriptor.name()))
                .sorted(Comparator.comparing(descriptor -> requireNonNullElse(descriptor.ruleBuilderName(), descriptor.name())))
                .toList();

        return Response.ok(Map.of("conditions", conditions)).build();
    }

    @POST
    @Path("/validate")
    @ApiOperation("Validate rule builder")
    @NoAuditEvent("No data changes. Only used to validate a rule builder.")
    public Response validateRule(@ApiParam(name = "rule", required = true) @NotNull RuleBuilderDto ruleBuilderDto) {
        final var validatedDto = validatorService.validate(ruleBuilderDto);
        final var dtoWithTitles = validatedDto.toBuilder()
                .ruleBuilder(ruleBuilderService.generateTitles(validatedDto.ruleBuilder()))
                .build();

        return Response.ok(Map.of("rule_builder", dtoWithTitles)).build();
    }

    @POST
    @Path("/simulate")
    @ApiOperation(value = "Run the simulator for the given rule and message")
    @NoAuditEvent("No data changes. Only used to simulate a filter rule.")
    @RequiresPermissions(RestPermissions.STREAM_DESTINATION_FILTERS_READ)
    public Response simulateRule() {
        throw new ServerErrorException("Simulator not implemented yet", Response.Status.NOT_IMPLEMENTED);
    }
}
