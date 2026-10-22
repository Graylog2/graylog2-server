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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.CachingDecorator;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.FieldDecorator;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.IdDecorator;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MessagesTabularResponseCreator implements TabularResponseCreator {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesTabularResponseCreator.class);

    private final MappedFieldTypesService mappedFieldTypesService;
    private final Set<FieldDecorator> decorators;

    @Inject
    public MessagesTabularResponseCreator(final MappedFieldTypesService mappedFieldTypesService, Set<FieldDecorator> decorators) {
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.decorators = decorators;
    }

    public TabularResponse mapToResponse(final MessagesRequestSpec messagesRequestSpec,
                                         final SearchJob searchJob,
                                         final SearchUser searchUser) throws QueryFailedException {
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(SearchRequestSpecToSearchMapper.QUERY_ID);

        if (queryResult != null) {
            throwErrorIfAnyAvailable(queryResult);
            final SearchType.Result messageListResult = queryResult.searchTypes().get(MessagesSpecToMessageListMapper.MESSAGE_LIST_ID);
            if (messageListResult instanceof MessageList.Result messagesResult) {
                return mapToResponse(messagesRequestSpec, messagesResult, searchUser);
            }
        }

        LOG.warn("Scripting API failed to obtain messages for input : " + messagesRequestSpec);
        throw new QueryFailedException("Scripting API failed to obtain messages for input : " + messagesRequestSpec);
    }

    private TabularResponse mapToResponse(final MessagesRequestSpec searchRequestSpec,
                                          final MessageList.Result messageListResult,
                                          final SearchUser searchUser) {
        final Set<String> streams = searchUser.streams().readableOrAllIfEmpty(searchRequestSpec.streams());
        final Set<MappedFieldTypeDTO> knownFields = mappedFieldTypesService.fieldTypesByStreamIds(streams, searchRequestSpec.timerange());
        final List<RequestedField> fields = getRequestedFields(searchRequestSpec, knownFields);
        return new TabularResponse(
                getSchema(fields, knownFields),
                getDatarows(fields, messageListResult, searchUser),
                new Metadata(messageListResult.effectiveTimerange())
        );
    }

    private List<ResponseSchemaEntry> getSchema(List<RequestedField> fields, Set<MappedFieldTypeDTO> knownFields) {
        final MessageFieldTypeMapper fieldsMapper = new MessageFieldTypeMapper(knownFields);
        return fields.stream()
                .map(fieldsMapper)
                .collect(Collectors.toList());
    }

    private List<RequestedField> getRequestedFields(MessagesRequestSpec searchRequestSpec, Set<MappedFieldTypeDTO> knownFields) {
        final List<RequestedField> fields = searchRequestSpec.requestedFields().isEmpty()
                ? knownFields.stream().map(MappedFieldTypeDTO::name).map(RequestedField::parse).toList()
                : searchRequestSpec.requestedFields();
        if (!searchRequestSpec.includeUnresolvedIds()) {
            return fields;
        }
        final List<RequestedField> fieldsWithUnresolvedIds = new ArrayList<>();
        final Set<RequestedField> addedUnresolvedIds = new HashSet<>();
        for (RequestedField field : fields) {
            fieldsWithUnresolvedIds.add(field);
            unresolvedId(field)
                    .filter(unresolvedId -> !fields.contains(unresolvedId) && addedUnresolvedIds.add(unresolvedId))
                    .ifPresent(fieldsWithUnresolvedIds::add);
        }
        return fieldsWithUnresolvedIds;
    }

    // Ids are resolved to titles by default, the unresolved ids are available through the id decorator.
    private Optional<RequestedField> unresolvedId(RequestedField field) {
        final RequestedField unresolvedId = new RequestedField(field.name(), IdDecorator.DECORATOR);
        return !field.hasDecorator(IdDecorator.DECORATOR) && isDecorated(field) && isDecorated(unresolvedId)
                ? Optional.of(unresolvedId)
                : Optional.empty();
    }

    private boolean isDecorated(RequestedField field) {
        return decorators.stream().anyMatch(decorator -> decorator.accept(field));
    }

    private List<List<Object>> getDatarows(final List<RequestedField> fields,
                                           final MessageList.Result messageListResult, SearchUser searchUser) {

        final Set<FieldDecorator> cachedDecorators = this.decorators.stream().map(CachingDecorator::new).collect(Collectors.toSet());

        return messageListResult.messages()
                .stream()
                .map(message -> fields
                        .stream()
                        .map(field -> extractValue(message, field, cachedDecorators, searchUser))
                        .collect(Collectors.toList())).collect(Collectors.toList());
    }

    private Object extractValue(ResultMessageSummary message, RequestedField field, Set<FieldDecorator> decorators, SearchUser searchUser) {
        return Optional.ofNullable(message.message().get(field.name()))
                .map(value -> decorate(decorators, field, value, searchUser))
                .orElse("-");
    }
}
