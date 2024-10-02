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
import org.graylog.plugins.views.search.explain.DataRoutedStream;
import org.graylog.plugins.views.search.explain.StreamQueryExplainer;
import org.graylog.plugins.views.search.explain.StreamQueryInfo;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.validation.QueryValidationService;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationResponse;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.streams.StreamService;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static org.graylog.plugins.views.search.ExplainResults.IndexRangeResult.fromIndexRange;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Search/Validation", tags = {CLOUD_VISIBLE})
@Path("/search/validate")
public class QueryValidationResource extends RestResource implements PluginRestResource {

    private final QueryValidationService queryValidationService;
    private final Optional<StreamQueryExplainer> optionalStreamQueryExplainer;
    private final IndexLookup indexLookup;
    private final StreamService streamService;

    @Inject
    public QueryValidationResource(final QueryValidationService queryValidationService,
                                   final Optional<StreamQueryExplainer> optionalStreamQueryExplainer,
                                   final IndexLookup indexLookup,
                                   final StreamService streamService) {
        this.queryValidationService = queryValidationService;
        this.optionalStreamQueryExplainer = optionalStreamQueryExplainer;
        this.indexLookup = indexLookup;
        this.streamService = streamService;
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
        final Set<ExplainResults.IndexRangeResult> searchedIndexRanges = indexRanges(request);
        final Optional<Set<String>> requestedStreams = validationRequest.streams();
        final Set<String> readableStreams = request.streams();
        final Set<DataRoutedStream> dataRoutedStreams = checkForDataRoutedStreams(requestedStreams, readableStreams, request.timerange(), request.isEmptyQuery());
        final boolean hasDataRoutedStreams = dataRoutedStreams != null && !dataRoutedStreams.isEmpty();
        final Optional<TimeRange> requestedRangeAsAbsolut = validationRequest.timerange().map(timeRange -> AbsoluteRange.create(timeRange.getFrom(), timeRange.getTo()));
        return ValidationResponseDTO.create(
                toStatus(response.status(), containsWarmIndices(searchedIndexRanges), hasDataRoutedStreams),
                toExplanations(response),
                searchedIndexRanges,
                dataRoutedStreams,
                requestedRangeAsAbsolut);
    }

    private boolean containsWarmIndices(Set<ExplainResults.IndexRangeResult> searchedIndexRanges) {
        return searchedIndexRanges.stream().anyMatch(ExplainResults.IndexRangeResult::isWarmTiered);
    }

    Set<DataRoutedStream> checkForDataRoutedStreams(Optional<Set<String>> requestedStreams, Set<String> readableStreams, TimeRange timeRange, boolean isEmptyQuery) {
        return optionalStreamQueryExplainer.map(streamQueryExplainer -> {
            Instant from = timeRange.getFrom().toInstant().toDate().toInstant();
            Instant to = timeRange.getTo().toInstant().toDate().toInstant();
            StreamQueryInfo query = new StreamQueryInfo(requestedStreams, readableStreams, from, to, isEmptyQuery);
            return optionalStreamQueryExplainer.get().explain(query);
        }).orElse(Set.of());
    }

    private ValidationRequest prepareRequest(ValidationRequestDTO validationRequest, SearchUser searchUser) {
        // Combine stream IDs mapped from streamCategories and stream IDs from the validationRequest before calling
        // readableOrAllIfEmpty to ensure all possible requested streams are added first.
        final Set<String> streamsAndMappedCategories = validationRequest.streams().orElse(new HashSet<>());
        if (validationRequest.streamCategories().isPresent()) {
            streamsAndMappedCategories.addAll(searchUser.streams().loadStreamsWithCategories(validationRequest.streamCategories().get()));
        }
        ImmutableSet.Builder<String> allRequestedStreams = ImmutableSet.<String>builder()
                .addAll(searchUser.streams().readableOrAllIfEmpty(streamsAndMappedCategories));

        final ValidationRequest.Builder q = ValidationRequest.Builder.builder()
                .query(validationRequest.query())
                .timerange(validationRequest.timerange().orElse(defaultTimeRange()))
                .streams(allRequestedStreams.build())
                .parameters(resolveParameters(validationRequest))
                .validationMode(validationRequest.validationMode().toInternalRepresentation());

        validationRequest.filter().ifPresent(q::filter);
        return q.build();
    }

    private Set<ExplainResults.IndexRangeResult> indexRanges(ValidationRequest request) {
        final Set<String> streamTitles = request.streams().stream()
                .map(streamService::streamTitleFromCache)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return indexLookup.indexRangesForStreamsInTimeRange(request.streams(), request.timerange()).stream()
                .map(indexRange -> fromIndexRange(indexRange, streamTitles))
                .collect(Collectors.toSet());
    }

    private ImmutableSet<Parameter> resolveParameters(final ValidationRequestDTO validationRequest) {
        return validationRequest.parameters().stream()
                .map(param -> param.applyBindings(validationRequest.parameterBindings()))
                .collect(toImmutableSet());
    }

    private ValidationStatusDTO toStatus(final ValidationStatus status, boolean hasWarmIndices, boolean hasDataRoutedStreams) {
        ValidationStatusDTO validationStatusDTO = switch (status) {
            case WARNING -> ValidationStatusDTO.WARNING;
            case ERROR -> ValidationStatusDTO.ERROR;
            default -> ValidationStatusDTO.OK;
        };

        boolean isOk = validationStatusDTO == ValidationStatusDTO.OK;

        if (isOk && hasWarmIndices) {
            return ValidationStatusDTO.WARNING;
        }

        if (isOk && hasDataRoutedStreams) {
            return ValidationStatusDTO.INFO;
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
