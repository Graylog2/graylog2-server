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
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionRequest;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionResponse;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionEntryDTO;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionsDTO;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionsErrorDTO;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionsRequestDTO;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Search/Suggestions", tags = {CLOUD_VISIBLE})
@Path("/search/suggest")
public class SuggestionsResource extends RestResource implements PluginRestResource {

    public static final int SUGGESTIONS_COUNT_MAX = 100;
    private final PermittedStreams permittedStreams;
    private final QuerySuggestionsService querySuggestionsService;

    @Inject
    public SuggestionsResource(PermittedStreams permittedStreams, QuerySuggestionsService querySuggestionsService) {
        this.permittedStreams = permittedStreams;
        this.querySuggestionsService = querySuggestionsService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Suggest field value")
    @NoAuditEvent("Only suggesting field value for query, not changing any data")
    public SuggestionsDTO suggestFieldValue(@ApiParam(name = "validationRequest") SuggestionsRequestDTO suggestionsRequest,
                                            @Context SearchUser searchUser) {

        final SuggestionRequest req = SuggestionRequest.builder()
                .field(suggestionsRequest.field())
                .input(suggestionsRequest.input())
                .streams(adaptStreams(suggestionsRequest.streams(), searchUser))
                .size(Math.min(suggestionsRequest.size(), SUGGESTIONS_COUNT_MAX))
                .timerange(Optional.ofNullable(suggestionsRequest.timerange()).orElse(defaultTimeRange()))
                .build();

        SuggestionResponse res = querySuggestionsService.suggest(req);
        final List<SuggestionEntryDTO> suggestions = res.suggestions().stream().map(s -> SuggestionEntryDTO.create(s.getValue(), s.getOccurrence())).collect(Collectors.toList());
        final SuggestionsDTO.Builder suggestionsBuilder = SuggestionsDTO.builder(res.field(), res.input())
                .suggestions(suggestions)
                .sumOtherDocsCount(res.sumOtherDocsCount());

        res.suggestionError()
                .map(e -> SuggestionsErrorDTO.create(e.type(), e.reason()))
                .ifPresent(suggestionsBuilder::error);

        return suggestionsBuilder.build();
    }

    private Set<String> adaptStreams(Set<String> streams, SearchUser searchUser) {
        if (streams == null || streams.isEmpty()) {
            return loadAllAllowedStreamsForUser(searchUser);
        } else {
            // TODO: is it ok to filter out a stream that's not accessible or should we throw an exception?
            return streams.stream().filter(searchUser::canReadStream).collect(Collectors.toSet());
        }
    }

    private RelativeRange defaultTimeRange() {
        try {
            return RelativeRange.create(300);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }

    private ImmutableSet<String> loadAllAllowedStreamsForUser(SearchUser searchUser) {
        return permittedStreams.load(searchUser::canReadStream);
    }
}
