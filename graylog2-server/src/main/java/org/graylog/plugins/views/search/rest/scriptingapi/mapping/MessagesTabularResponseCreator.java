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

import org.apache.shiro.subject.Subject;
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
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.shared.utilities.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
                                         final SearchUser searchUser, Subject subject) throws QueryFailedException {
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(SearchRequestSpecToSearchMapper.QUERY_ID);

        if (queryResult != null) {
            throwErrorIfAnyAvailable(queryResult);
            final SearchType.Result messageListResult = queryResult.searchTypes().get(MessagesSpecToMessageListMapper.MESSAGE_LIST_ID);
            if (messageListResult instanceof MessageList.Result messagesResult) {
                return mapToResponse(messagesRequestSpec, messagesResult, searchUser, subject);
            }
        }

        LOG.warn("Scripting API failed to obtain messages for input : " + messagesRequestSpec);
        throw new QueryFailedException("Scripting API failed to obtain messages for input : " + messagesRequestSpec);
    }

    private TabularResponse mapToResponse(final MessagesRequestSpec searchRequestSpec,
                                          final MessageList.Result messageListResult,
                                          final SearchUser searchUser, Subject subject) {
        return new TabularResponse(
                getSchema(searchRequestSpec, searchUser),
                getDatarows(searchRequestSpec, messageListResult, subject),
                new Metadata(messageListResult.effectiveTimerange())
        );
    }

    private List<ResponseSchemaEntry> getSchema(MessagesRequestSpec searchRequestSpec, SearchUser searchUser) {
        final Set<String> streams = searchUser.streams().readableOrAllIfEmpty(searchRequestSpec.streams());
        final Set<MappedFieldTypeDTO> knownFields = mappedFieldTypesService.fieldTypesByStreamIds(streams, searchRequestSpec.timerange());
        final MessageFieldTypeMapper fieldsMapper = new MessageFieldTypeMapper(knownFields);

        return searchRequestSpec.requestedFields()
                .stream()
                .map(fieldsMapper)
                .collect(Collectors.toList());
    }

    private List<List<Object>> getDatarows(final MessagesRequestSpec messagesRequestSpec,
                                           final MessageList.Result messageListResult, Subject subject) {

        final Set<FieldDecorator> cachedDecorators = this.decorators.stream().map(CachingDecorator::new).collect(Collectors.toSet());

        return messageListResult.messages()
                .stream()
                .map(message -> messagesRequestSpec.requestedFields()
                        .stream()
                        .map(field -> extractValue(message, field, cachedDecorators, subject))
                        .collect(Collectors.toList())).collect(Collectors.toList());
    }

    private Object extractValue(ResultMessageSummary message, RequestedField field, Set<FieldDecorator> decorators, Subject subject) {
        return Optional.ofNullable(message.message().get(field.name()))
                .map(value -> decorate(decorators, field, value, subject))
                .orElse("-");
    }

    private Object decorate(Set<FieldDecorator> decorators, RequestedField field, Object val, Subject subject) {
        final Optional<Object> decorated = decorators.stream()
                .filter(d -> d.accept(field))
                .findFirst()
                .map(d -> d.decorate(field, val, subject));

        if (decorated.isEmpty() && field.hasDecorator()) {
            throw new IllegalArgumentException(StringUtils.f("Unsupported decorator '%s' on field '%s'", field.decorator(), field.name()));
        } else {
            return decorated.orElse(val);
        }
    }
}
