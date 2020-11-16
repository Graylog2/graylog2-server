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
package org.graylog2.plugin.decorators;

import org.graylog2.plugin.Message;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractMessageDecorator implements SearchResponseDecorator {
    abstract Message decorate(Message message);

    @Override
    public SearchResponse apply(SearchResponse searchResponse) {
        final List<ResultMessageSummary> results = searchResponse.messages().stream()
            .map(resultMessageSummary -> {
                final Message decoratedMessage = decorate(new Message(resultMessageSummary.message()));
                return ResultMessageSummary.create(resultMessageSummary.highlightRanges(), decoratedMessage.getFields(), resultMessageSummary.index());
            })
            .collect(Collectors.toList());

        return searchResponse.toBuilder().messages(results).build();
    }
}
