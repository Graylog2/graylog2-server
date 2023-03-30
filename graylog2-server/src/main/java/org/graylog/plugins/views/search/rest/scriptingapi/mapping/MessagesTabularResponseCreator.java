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
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MessagesTabularResponseCreator implements TabularResponseCreator {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesTabularResponseCreator.class);

    private final FieldTypesLookup fieldTypesLookup;

    @Inject
    public MessagesTabularResponseCreator(final FieldTypesLookup fieldTypesLookup) {
        this.fieldTypesLookup = fieldTypesLookup;
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
        final Map<String, String> fieldTypes = fieldTypesLookup.getTypes(streams, new HashSet<>(searchRequestSpec.fields()));

        return new TabularResponse(
                searchRequestSpec.getSchema(fieldTypes),
                getDatarows(searchRequestSpec, messageListResult),
                new Metadata(messageListResult.effectiveTimerange())
        );
    }

    private static List<List<Object>> getDatarows(final MessagesRequestSpec messagesRequestSpec,
                                                  final MessageList.Result messageListResult) {
        return messageListResult.messages()
                .stream()
                .map(message -> messagesRequestSpec.fields()
                        .stream()
                        .map(field -> message.message().get(field))
                        .map(value -> value == null ? "-" : value)
                        .collect(Collectors.toList())).collect(Collectors.toList());
    }

}
