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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.validation.QueryValidationService;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationResponse;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

@RequiresAuthentication
@Api(value = "Search/Validation")
@Path("/search/validate")
public class QueryValidationResource extends RestResource implements PluginRestResource {

    private final QueryValidationService queryValidationService;

    @Inject
    public QueryValidationResource(QueryValidationService queryValidationService) {
        this.queryValidationService = queryValidationService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Validate a search query")
    @NoAuditEvent("Only validating query structure, not changing any data")
    public ValidationResponseDTO validateQuery(
            @ApiParam(name = "validationRequest") ValidationRequestDTO validationRequest,
            @Context SearchUser searchUser
    ) {

        final ValidationRequest.Builder q = ValidationRequest.Builder.builder()
                .query(validationRequest.query())
                .timerange(validationRequest.timerange().orElse(defaultTimeRange()))
                .streams(searchUser.streams().readableOrAllIfEmpty(validationRequest.streams()))
                .parameters(resolveParameters(validationRequest));

        validationRequest.filter().ifPresent(q::filter);

        final ValidationResponse response = queryValidationService.validate(q.build());
        return ValidationResponseDTO.create(toStatus(response.status()), toExplanations(response));
    }

    private ImmutableSet<Parameter> resolveParameters(ValidationRequestDTO validationRequest) {
        return validationRequest.parameters().stream()
                .map(param -> param.applyBindings(validationRequest.parameterBindings()))
                .collect(toImmutableSet());
    }

    private ValidationStatusDTO toStatus(ValidationStatus status) {
        final ValidationStatusDTO statusDTO;
        switch (status) {
            case WARNING:
                statusDTO = ValidationStatusDTO.WARNING;
                break;
            case ERROR:
                statusDTO = ValidationStatusDTO.ERROR;
                break;
            default:
                statusDTO = ValidationStatusDTO.OK;
        }
        return statusDTO;
    }

    private List<ValidationMessageDTO> toExplanations(ValidationResponse response) {
        return response.explanations().stream()
                .map(this::toExplanation)
                .sorted(Comparator.comparing(ValidationMessageDTO::beginLine, nullsLast(naturalOrder()))
                        .thenComparing(ValidationMessageDTO::beginColumn, nullsLast(naturalOrder())))
                .collect(Collectors.toList());
    }

    private ValidationMessageDTO toExplanation(ValidationMessage message) {
        final ValidationTypeDTO validationType = convert(message.validationType());
        final ValidationMessageDTO.Builder builder = ValidationMessageDTO.builder(
                validationType,
                message.errorMessage());

        message.relatedProperty().ifPresent(builder::relatedProperty);
        message.position().ifPresent(queryPosition -> {
            builder.beginLine(queryPosition.beginLine());
            builder.beginColumn(queryPosition.beginColumn());
            builder.endLine(queryPosition.endLine());
            builder.endColumn(queryPosition.endColumn());
        });
        return builder.build();
    }

    private ValidationTypeDTO convert(ValidationType validationType) {
        return ValidationTypeDTO.from(validationType);
    }

    private RelativeRange defaultTimeRange() {
        try {
            return RelativeRange.create(300);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }
}
