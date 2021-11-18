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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.engine.ValidationExplanation;
import org.graylog.plugins.views.search.engine.ValidationRequest;
import org.graylog.plugins.views.search.engine.ValidationResponse;
import org.graylog.plugins.views.search.engine.ValidationStatus;
import org.graylog.plugins.views.search.engine.validation.ValidationMessageParser;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

@RequiresAuthentication
@Api(value = "Search/Validation")
@Path("/search/validate")
public class QueryValidationResource extends RestResource implements PluginRestResource {

    private final QueryEngine queryEngine;
    private final PermittedStreams permittedStreams;
    private final ObjectMapper objectMapper;

    @Inject
    public QueryValidationResource(QueryEngine queryEngine, PermittedStreams permittedStreams, ObjectMapper objectMapper) {
        this.queryEngine = queryEngine;
        this.permittedStreams = permittedStreams;
        this.objectMapper = objectMapper;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Validate a search query")
    @NoAuditEvent("Only validating query structure, not changing any data")
    public ValidationResponseDTO validateQuery(@ApiParam(name = "validationRequest") ValidationRequestDTO validationRequest) {

        final ValidationRequest q = ValidationRequest.Builder.builder()
                .query(validationRequest.query())
                .timerange(Optional.ofNullable(validationRequest.timerange()).orElse(defaultTimeRange()))
                .streams(adaptStreams(validationRequest.streams()))
                .parameters(resolveParameters(validationRequest))
                .build();

        final ValidationResponse response = queryEngine.validate(q);
        return ValidationResponseDTO.create(toStatus(response.getStatus()), toExplanations(response), response.getUnknownFields());
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

    private List<ValidationExplanationDTO> toExplanations(ValidationResponse response) {
        if (ValidationStatus.OK.equals(response.getStatus())) {
            return Collections.emptyList();
        }
        return response.getExplanations().stream()
                .map(this::toExplanation)
                .collect(Collectors.toList());
    }

    private ValidationExplanationDTO toExplanation(ValidationExplanation e) {
        final ValidationExplanationDTO.Builder explanation = ValidationExplanationDTO.builder()
                .index(e.getIndex())
                .valid(e.isValid());

        ValidationMessageParser.getHumanReadableMessage(firstNonNull(e.getExplanation(), e.getError()))
                .ifPresent(explanation::message);

        return explanation.build();
    }

    private Set<String> adaptStreams(Set<String> streams) {
        if (streams == null || streams.isEmpty()) {
            return loadAllAllowedStreamsForUser();
        } else {
            // TODO: is it ok to filter out a stream that's not accessible or should we throw an exception?
            return streams.stream().filter(this::hasStreamReadPermission).collect(Collectors.toSet());
        }
    }

    private RelativeRange defaultTimeRange() {
        try {
            return RelativeRange.create(300);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }

    private ImmutableSet<String> loadAllAllowedStreamsForUser() {
        return permittedStreams.load(this::hasStreamReadPermission);
    }

    private boolean hasStreamReadPermission(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }

    protected boolean isPermitted(String permission, String instanceId) {
        return getSubject().isPermitted(permission + ":" + instanceId);
    }
}
