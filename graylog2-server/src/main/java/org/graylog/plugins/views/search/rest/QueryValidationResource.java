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
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.ExplainResults;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.validation.QueryValidationService;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationResponse;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Search/Validation", tags = {CLOUD_VISIBLE})
@Path("/search/validate")
public class QueryValidationResource extends RestResource implements PluginRestResource {

    private final QueryValidationService queryValidationService;

    private final IndexLookup indexLookup;

    @Inject
    public QueryValidationResource(final QueryValidationService queryValidationService,
                                   final IndexLookup indexLookup) {
        this.queryValidationService = queryValidationService;
        this.indexLookup = indexLookup;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Validate a search query")
    @NoAuditEvent("Only validating query structure, not changing any data")
    public ValidationResponseDTO validateQuery(
            @ApiParam(name = "validationRequest") final ValidationRequestDTO validationRequest,
            @Context final SearchUser searchUser
    ) {
        ValidationRequest request = prepareRequest(validationRequest, searchUser);
        final ValidationResponse response = queryValidationService.validate(request);
        Set<ExplainResults.IndexRangeResult> searchedIndexRanges = indexRanges(request);
        return ValidationResponseDTO.create(
                toStatus(response.status(), containsWarmIndices(searchedIndexRanges)),
                toExplanations(response),
                searchedIndexRanges);
    }

    private boolean containsWarmIndices(Set<ExplainResults.IndexRangeResult> searchedIndexRanges) {
        return searchedIndexRanges.stream().anyMatch(ExplainResults.IndexRangeResult::isWarmTiered);
    }

    private ValidationRequest prepareRequest(ValidationRequestDTO validationRequest, SearchUser searchUser) {
        final ValidationRequest.Builder q = ValidationRequest.Builder.builder()
                .query(validationRequest.query())
                .timerange(validationRequest.timerange().orElse(defaultTimeRange()))
                .streams(searchUser.streams().readableOrAllIfEmpty(validationRequest.streams()))
                .parameters(resolveParameters(validationRequest))
                .validationMode(validationRequest.validationMode().toInternalRepresentation());

        validationRequest.filter().ifPresent(q::filter);
        return q.build();
    }

    private Set<ExplainResults.IndexRangeResult> indexRanges(ValidationRequest request) {
        return indexLookup.indexRangesForStreamsInTimeRange(request.streams(), request.timerange()).stream()
                .map(ExplainResults.IndexRangeResult::fromIndexRange)
                .collect(Collectors.toSet());
    }

    private ImmutableSet<Parameter> resolveParameters(final ValidationRequestDTO validationRequest) {
        return validationRequest.parameters().stream()
                .map(param -> param.applyBindings(validationRequest.parameterBindings()))
                .collect(toImmutableSet());
    }

    private ValidationStatusDTO toStatus(final ValidationStatus status, boolean hasWarmIndices) {
        ValidationStatusDTO validationStatusDTO = switch (status) {
            case WARNING -> ValidationStatusDTO.WARNING;
            case ERROR -> ValidationStatusDTO.ERROR;
            default -> ValidationStatusDTO.OK;
        };
        if (validationStatusDTO == ValidationStatusDTO.OK) {
            return hasWarmIndices ? ValidationStatusDTO.WARNING : ValidationStatusDTO.OK;
        }
        return validationStatusDTO;
    }

    private List<ValidationMessageDTO> toExplanations(final ValidationResponse response) {
        return response.explanations().stream()
                .map(this::toExplanation)
                .sorted(Comparator.comparing(ValidationMessageDTO::beginLine, nullsLast(naturalOrder()))
                        .thenComparing(ValidationMessageDTO::beginColumn, nullsLast(naturalOrder())))
                .collect(Collectors.toList());
    }

    private ValidationMessageDTO toExplanation(final ValidationMessage message) {
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

    private ValidationTypeDTO convert(final ValidationType validationType) {
        return ValidationTypeDTO.from(validationType);
    }

    private RelativeRange defaultTimeRange() {
        return RelativeRange.create(300);
    }
}
