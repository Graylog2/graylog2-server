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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionFieldType;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionRequest;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionResponse;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.querystrings.LastUsedQueryStringsService;
import org.graylog.plugins.views.search.querystrings.QueryString;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionEntryDTO;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionsDTO;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionsErrorDTO;
import org.graylog.plugins.views.search.rest.suggestions.SuggestionsRequestDTO;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleService;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.plugins.views.search.querystrings.LastUsedQueryStringsService.DEFAULT_LIMIT;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Search/Suggestions", tags = {CLOUD_VISIBLE})
@Path("/search/suggest")
@Produces(MediaType.APPLICATION_JSON)
public class SuggestionsResource extends RestResource implements PluginRestResource {

    public static final int SUGGESTIONS_COUNT_MAX = 100;
    private final PermittedStreams permittedStreams;
    private final QuerySuggestionsService querySuggestionsService;

    private final MappedFieldTypesService mappedFieldTypesService;

    private final EntityTitleService entityTitleService;
    private final NodeService nodeService;
    private final LastUsedQueryStringsService lastUsedQueryStringsService;

    @Inject
    public SuggestionsResource(PermittedStreams permittedStreams, QuerySuggestionsService querySuggestionsService,
                               MappedFieldTypesService mappedFieldTypesService, EntityTitleService entityTitleService,
                               NodeService nodeService, LastUsedQueryStringsService lastUsedQueryStringsService) {
        this.permittedStreams = permittedStreams;
        this.querySuggestionsService = querySuggestionsService;
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.entityTitleService = entityTitleService;
        this.nodeService = nodeService;
        this.lastUsedQueryStringsService = lastUsedQueryStringsService;
    }

    @GET
    @Path("/query_strings")
    @ApiOperation("Suggest last used query strings")
    public List<QueryString> suggestQueryStrings(@Context SearchUser searchUser,
                                                 @ApiParam("limit") @QueryParam("limit") Integer limit) {
        return lastUsedQueryStringsService.get(searchUser.getUser(), Optional.ofNullable(limit).orElse(DEFAULT_LIMIT));
    }

    @POST
    @ApiOperation("Suggest field value")
    @NoAuditEvent("Only suggesting field value for query, not changing any data")
    public SuggestionsDTO suggestFieldValue(@ApiParam(name = "validationRequest") SuggestionsRequestDTO suggestionsRequest,
                                            @Context SearchUser searchUser) {

        final Set<String> streams = adaptStreams(suggestionsRequest.streams(), searchUser);
        final TimeRange timerange = Optional.ofNullable(suggestionsRequest.timerange()).orElse(defaultTimeRange());
        final String fieldName = suggestionsRequest.field();

        final Set<MappedFieldTypeDTO> fieldTypes = mappedFieldTypesService.fieldTypesByStreamIds(streams, timerange);
        var fieldType = fieldTypes.stream().filter(f -> f.name().equals(fieldName))
                .findFirst()
                .map(MappedFieldTypeDTO::type)
                .orElse(FieldTypes.Type.createType("unknown", Collections.emptySet()));

        final SuggestionRequest req = SuggestionRequest.builder()
                .field(fieldName)
                .fieldType(getFieldType(streams, timerange, fieldName))
                .input(suggestionsRequest.input())
                .streams(streams)
                .size(Math.min(suggestionsRequest.size(), SUGGESTIONS_COUNT_MAX))
                .timerange(timerange)
                .build();

        SuggestionResponse res = querySuggestionsService.suggest(req);
        final List<SuggestionEntryDTO> suggestions = augmentSuggestions(res.suggestions().stream()
                .map(s -> SuggestionEntryDTO.create(s.getValue(), s.getOccurrence()))
                .toList(), fieldType, searchUser);
        final SuggestionsDTO.Builder suggestionsBuilder = SuggestionsDTO.builder(res.field(), res.input())
                .suggestions(suggestions)
                .sumOtherDocsCount(res.sumOtherDocsCount());

        res.suggestionError()
                .map(e -> SuggestionsErrorDTO.create(e.type(), e.reason()))
                .ifPresent(suggestionsBuilder::error);

        return suggestionsBuilder.build();
    }

    private List<SuggestionEntryDTO> augmentSuggestions(List<SuggestionEntryDTO> suggestions, FieldTypes.Type fieldType, SearchUser searchUser) {
        if (fieldType.equals(FieldTypeMapper.STREAMS_TYPE) || fieldType.equals(FieldTypeMapper.INPUT_TYPE)) {
            var entityIds = suggestions.stream()
                    .map(SuggestionEntryDTO::value)
                    .distinct()
                    .map(value -> new EntityIdentifier(value, mapEntityType(fieldType.type())))
                    .toList();
            var results = entityTitleService.getTitles(new EntityTitleRequest(entityIds), searchUser).entities()
                    .stream()
                    .collect(Collectors.toMap(EntityTitleResponse::id, EntityTitleResponse::title));
            return suggestions.stream()
                    .map(s -> SuggestionEntryDTO.create(s.value(), s.occurrence(), Optional.ofNullable(results.get(s.value()))))
                    .toList();
        }

        if (fieldType.equals(FieldTypeMapper.NODE_TYPE)) {
            var nodeIds = suggestions.stream()
                    .map(SuggestionEntryDTO::value)
                    .distinct()
                    .toList();

            var results = nodeService.byNodeIds(nodeIds);
            return suggestions.stream()
                    .map(s -> SuggestionEntryDTO.create(
                            s.value(),
                            s.occurrence(),
                            Optional.ofNullable(results.get(s.value())).map(Node::getTitle)
                    ))
                    .toList();
        }

        return suggestions;
    }

    private String mapEntityType(String type) {
        return switch (type) {
            case "streams" -> "streams";
            case "input" -> "inputs";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
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
        return RelativeRange.create(300);
    }

    private ImmutableSet<String> loadAllAllowedStreamsForUser(SearchUser searchUser) {
        return permittedStreams.loadAllMessageStreams(searchUser);
    }

    private SuggestionFieldType getFieldType(Set<String> streams, TimeRange timerange, final String fieldName) {
        final Set<MappedFieldTypeDTO> fieldTypes = mappedFieldTypesService.fieldTypesByStreamIds(streams, timerange);
        return fieldTypes.stream().filter(f -> f.name().equals(fieldName))
                .findFirst()
                .map(MappedFieldTypeDTO::type)
                .map(SuggestionFieldType::fromFieldType)
                .orElse(SuggestionFieldType.OTHER);
    }
}
