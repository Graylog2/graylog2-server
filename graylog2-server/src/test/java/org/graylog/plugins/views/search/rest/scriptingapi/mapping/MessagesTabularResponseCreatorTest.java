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
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.FieldDecorator;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.IdDecorator;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.Message;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.fieldtypes.FieldTypeMapper.STREAMS_TYPE;
import static org.graylog2.indexer.fieldtypes.FieldTypeMapper.STRING_TYPE;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagesTabularResponseCreatorTest {
    private static final String STREAM_ID = "stream-1";

    private final MappedFieldTypesService mappedFieldTypesService = mock(MappedFieldTypesService.class);
    private final SearchUser searchUser = TestSearchUser.builder().allowStream(STREAM_ID).build();
    private MessagesTabularResponseCreator creator;

    @BeforeEach
    void setUp() {
        creator = new MessagesTabularResponseCreator(mappedFieldTypesService, Set.of(new IdDecorator(), new StreamTitleDecorator()));
        when(mappedFieldTypesService.fieldTypesByStreamIds(any(), any())).thenReturn(Set.of(
                MappedFieldTypeDTO.create("source", STRING_TYPE),
                MappedFieldTypeDTO.create(Message.FIELD_STREAMS, STREAMS_TYPE)));
    }

    @Test
    void returnsAllDecoratedFieldsWhenNoFieldsAreRequested() throws QueryFailedException {
        final TabularResponse response = creator.mapToResponse(spec(List.of(), false), searchJob(), searchUser);

        assertThat(columnNames(response)).containsExactlyInAnyOrder("source", "streams");
        assertThat(firstRow(response))
                .containsEntry("source", "example.org")
                .containsEntry("streams", "Title of stream-1");
    }

    @Test
    void addsUndecoratedFieldsToAllFields() throws QueryFailedException {
        final TabularResponse response = creator.mapToResponse(spec(List.of(), true), searchJob(), searchUser);

        assertThat(columnNames(response)).containsExactlyInAnyOrder("source", "streams", "streams#id");
        assertThat(firstRow(response))
                .containsEntry("source", "example.org")
                .containsEntry("streams", "Title of stream-1")
                .containsEntry("streams#id", List.of(STREAM_ID));
    }

    @Test
    void returnsOnlyRequestedFields() throws QueryFailedException {
        final TabularResponse response = creator.mapToResponse(spec(List.of("streams"), false), searchJob(), searchUser);

        assertThat(columnNames(response)).containsExactly("streams");
        assertThat(response.datarows()).containsExactly(List.of("Title of stream-1"));
    }

    @Test
    void addsUndecoratedFieldsNextToRequestedFields() throws QueryFailedException {
        final TabularResponse response = creator.mapToResponse(spec(List.of("streams", "source"), true), searchJob(), searchUser);

        assertThat(columnNames(response)).containsExactly("streams", "streams#id", "source");
        assertThat(response.datarows()).containsExactly(List.of("Title of stream-1", List.of(STREAM_ID), "example.org"));
    }

    @Test
    void doesNotDuplicateRequestedUndecoratedFields() throws QueryFailedException {
        final TabularResponse response = creator.mapToResponse(spec(List.of("streams", "streams#id"), true), searchJob(), searchUser);

        assertThat(columnNames(response)).containsExactly("streams", "streams#id");
    }

    private static MessagesRequestSpec spec(List<String> fields, boolean includeUndecorated) {
        return new MessagesRequestSpec(null, Set.of(STREAM_ID), Set.of(), null, null, null, 0, 10, fields, includeUndecorated);
    }

    private static SearchJob searchJob() {
        final ResultMessageSummary message = ResultMessageSummary.create(null,
                Map.of("source", "example.org", Message.FIELD_STREAMS, List.of(STREAM_ID)),
                "graylog_0");
        final MessageList.Result messageList = mock(MessageList.Result.class);
        when(messageList.messages()).thenReturn(List.of(message));

        final QueryResult queryResult = mock(QueryResult.class);
        when(queryResult.searchTypes()).thenReturn(Map.<String, SearchType.Result>of(MessagesSpecToMessageListMapper.MESSAGE_LIST_ID, messageList));

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJob.results()).thenReturn(Map.of(SearchRequestSpecToSearchMapper.QUERY_ID, queryResult));
        return searchJob;
    }

    private static List<String> columnNames(TabularResponse response) {
        return response.schema().stream().map(ResponseSchemaEntry::fieldName).toList();
    }

    private static Map<String, Object> firstRow(TabularResponse response) {
        final List<String> columns = columnNames(response);
        final List<Object> row = response.datarows().getFirst();
        final Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            values.put(columns.get(i), row.get(i));
        }
        return values;
    }

    private static class StreamTitleDecorator implements FieldDecorator {
        @Override
        public boolean accept(RequestedField field) {
            return Message.FIELD_STREAMS.equals(field.name()) && !field.hasDecorator();
        }

        @Override
        public Object decorate(RequestedField field, Object value, SearchUser searchUser) {
            return f("Title of %s", String.join(",", (List<String>) value));
        }
    }
}
