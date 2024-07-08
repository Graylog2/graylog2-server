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

import com.mongodb.client.model.Sorts;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidatorService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.rest.RuleBuilderDto;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.rest.PaginationParameters;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.filters.StreamDestinationFilterRuleDTO;
import org.graylog2.streams.filters.StreamDestinationFilterService;

import java.util.List;
import java.util.Map;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.shared.utilities.StringUtils.f;

@Api(value = "Stream/Destinations/Filters", description = "Manage stream destination filter rules", tags = {CLOUD_VISIBLE})
@Path("/streams/{streamId}/destinations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class StreamDestinationFiltersResource extends RestResource {
    private final StreamDestinationFilterService filterService;
    private final StreamService streamService;
    private final ValidatorService validatorService;

    @Inject
    public StreamDestinationFiltersResource(StreamDestinationFilterService filterService,
                                            StreamService streamService,
                                            ValidatorService validatorService) {
        this.filterService = filterService;
        this.streamService = streamService;
        this.validatorService = validatorService;
    }

    @GET
    @Path("/filters")
    @ApiOperation("Get available filter rules for stream")
    public PaginatedResponse<StreamDestinationFilterRuleDTO> getPaginatedFiltersForStream(
            @ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
            @ApiParam(name = "pagination parameters") @BeanParam PaginationParameters paginationParams
    ) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkStream(streamId);

        final var paginatedList = filterService.findPaginatedForStream(
                streamId,
                paginationParams.getQuery(),
                Sorts.ascending(StreamDestinationFilterRuleDTO.FIELD_TITLE),
                paginationParams.getPerPage(),
                paginationParams.getPage(),
                dtoId -> isPermitted(RestPermissions.STREAM_DESTINATION_FILTERS_READ, dtoId)
        );

        return PaginatedResponse.create("elements", paginatedList, paginationParams.getQuery());
    }

    @GET
    @Path("/target/{targetId}/filters")
    @ApiOperation("Get available filter rules for stream and target")
    public PaginatedResponse<StreamDestinationFilterRuleDTO> getPaginatedFiltersForStreamAndTarget(
            @ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
            @ApiParam(name = "targetId", required = true) @PathParam("targetId") @NotBlank String targetId,
            @ApiParam(name = "pagination parameters") @BeanParam PaginationParameters paginationParams
    ) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkStream(streamId);

        final var paginatedList = filterService.findPaginatedForStreamAndTarget(
                streamId,
                targetId,
                paginationParams.getQuery(),
                Sorts.ascending(StreamDestinationFilterRuleDTO.FIELD_TITLE),
                paginationParams.getPerPage(),
                paginationParams.getPage(),
                dtoId -> isPermitted(RestPermissions.STREAM_DESTINATION_FILTERS_READ, dtoId)
        );

        return PaginatedResponse.create("elements", paginatedList, paginationParams.getQuery());
    }

    @GET
    @Path("/filters/{filterId}")
    @ApiOperation("Get filter rule for given ID")
    public Response getFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                              @ApiParam(name = "filterId", required = true) @PathParam("filterId") @NotBlank String filterId) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_DESTINATION_FILTERS_READ, filterId);
        checkStream(streamId);

        final var dto = filterService.findByIdForStream(streamId, filterId)
                .orElseThrow(() -> new NotFoundException("Filter not found"));

        return Response.ok(wrapDto(dto)).build();
    }

    @POST
    @Path("/filters")
    @ApiOperation("Create new filter rule")
    @AuditEvent(type = AuditEventTypes.STREAM_DESTINATION_FILTER_CREATE)
    public Response createFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                                 @ApiParam(name = "JSON Body", required = true) @Valid StreamDestinationFilterRuleDTO dto) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_DESTINATION_FILTERS_CREATE);
        checkStream(streamId);
        validateDto(dto);

        try {
            return Response.ok(wrapDto(filterService.createForStream(streamId, dto))).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @PUT
    @Path("/filters/{filterId}")
    @ApiOperation(value = "Update filter rule")
    @AuditEvent(type = AuditEventTypes.STREAM_DESTINATION_FILTER_UPDATE)
    public Response updateFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                                 @ApiParam(name = "filterId", required = true) @PathParam("filterId") @NotBlank String filterId,
                                 @ApiParam(name = "JSON Body", required = true) @Valid StreamDestinationFilterRuleDTO dto) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_DESTINATION_FILTERS_EDIT, filterId);
        checkStream(streamId);

        if (!filterId.equals(dto.id())) {
            throw new BadRequestException("The filter ID in the URL doesn't match the one in the payload");
        }

        validateDto(dto);

        try {
            return Response.ok(wrapDto(filterService.updateForStream(streamId, dto))).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @DELETE
    @Path("/filters/{filterId}")
    @ApiOperation("Delete filter rule")
    @AuditEvent(type = AuditEventTypes.STREAM_DESTINATION_FILTER_DELETE)
    public Response deleteFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                                 @ApiParam(name = "filterId", required = true) @PathParam("filterId") @NotBlank String filterId) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_DESTINATION_FILTERS_DELETE, filterId);
        checkStream(streamId);

        try {
            return Response.ok(wrapDto(filterService.deleteFromStream(streamId, filterId))).build();
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    private Map<String, StreamDestinationFilterRuleDTO> wrapDto(StreamDestinationFilterRuleDTO dto) {
        return Map.of("filter", dto);
    }

    // We want to ensure that the given stream exists to avoid creating filter rules for non-existent streams.
    private void checkStream(String streamId) {
        try {
            streamService.load(streamId);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new NotFoundException(f("Stream not found: %s", streamId));
        }
    }

    private void validateDto(@Valid StreamDestinationFilterRuleDTO dto) {
        final var ruleBuilderDto = RuleBuilderDto.builder()
                .title(dto.title())
                .ruleBuilder(dto.rule().toBuilder().actions(List.of()).build())
                .build();
        try {
            validatorService.validateAndFailFast(ruleBuilderDto);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
