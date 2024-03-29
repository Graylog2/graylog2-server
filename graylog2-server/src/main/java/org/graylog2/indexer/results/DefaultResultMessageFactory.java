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
package org.graylog2.indexer.results;

import jakarta.inject.Inject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultResultMessageFactory implements ResultMessageFactory {
    private final MessageFactory messageFactory;

    @Inject
    public DefaultResultMessageFactory(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    @Override
    public ResultMessage parseFromSource(String id, String index, Map<String, Object> message) {
        return parseFromSource(id, index, message, Collections.emptyMap());
    }

    @Override
    public ResultMessage parseFromSource(String id, String index, Map<String, Object> message, Map<String, List<String>> highlight) {
        return new ResultMessage(messageFactory, id, index, message, HighlightParser.extractHighlightRanges(highlight));
    }

    @Override
    public ResultMessage createFromMessage(Message message) {
        ResultMessage m = new ResultMessage(messageFactory);
        m.setMessage(message);
        return m;
    }
}
