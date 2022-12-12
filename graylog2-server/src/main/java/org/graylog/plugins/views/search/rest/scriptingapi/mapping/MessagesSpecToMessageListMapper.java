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

import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.plugin.Message;

import java.util.List;
import java.util.function.Function;

public class MessagesSpecToMessageListMapper implements Function<MessagesRequestSpec, MessageList> {

    public static final String MESSAGE_LIST_ID = "scripting_api_temporary_message_list";

    @Override
    public MessageList apply(final MessagesRequestSpec messagesRequestSpec) {
        final MessageList.Builder messageListBuilder = MessageList.builder()
                .id(MESSAGE_LIST_ID)
                .sort(createSort(messagesRequestSpec))
                .limit(messagesRequestSpec.size())
                .offset(messagesRequestSpec.from())
                .fields(messagesRequestSpec.fields());

        return messageListBuilder
                .build();
    }

    private List<Sort> createSort(final MessagesRequestSpec messagesRequestSpec) {
        if (messagesRequestSpec.sort() == null || messagesRequestSpec.sort().equals(Message.FIELD_TIMESTAMP)) {
            //include recent Marco's trick to use "gl2_message_id" as additional sort field, as timestamps alone have only milisecond precision
            final Sort.Order order = messagesRequestSpec.sortOrder().toSortOrder();
            return List.of(
                    Sort.create(Message.FIELD_TIMESTAMP, order),
                    Sort.create(Message.FIELD_GL2_MESSAGE_ID, order)
            );
        } else {
            return List.of(Sort.create(messagesRequestSpec.sort(), messagesRequestSpec.sortOrder().toSortOrder()));
        }
    }
}
