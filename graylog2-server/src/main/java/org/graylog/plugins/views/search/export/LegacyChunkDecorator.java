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
package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableMultimap;
import org.graylog.plugins.views.search.LegacyDecoratorProcessor;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyChunkDecorator implements ChunkDecorator {

    private final LegacyDecoratorProcessor decoratorProcessor;

    @Inject
    public LegacyChunkDecorator(LegacyDecoratorProcessor decoratorProcessor) {
        this.decoratorProcessor = decoratorProcessor;
    }

    @Override
    public SimpleMessageChunk decorate(SimpleMessageChunk undecoratedChunk, ExportMessagesCommand command) {

        SearchResponse undecoratedLegacyResponse = legacySearchResponseFrom(undecoratedChunk, command);

        SearchResponse decoratedLegacyResponse = decoratorProcessor.decorateSearchResponse(undecoratedLegacyResponse, command.decorators());

        SimpleMessageChunk decoratedChunk = simpleMessageChunkFrom(decoratedLegacyResponse, undecoratedChunk.fieldsInOrder());

        return decoratedChunk.toBuilder().chunkOrder(undecoratedChunk.chunkOrder()).build();
    }

    private SimpleMessageChunk simpleMessageChunkFrom(SearchResponse searchResponse, LinkedHashSet<String> fieldsInOrder) {
        LinkedHashSet<SimpleMessage> messages = searchResponse.messages().stream()
                .map(legacyMessage -> SimpleMessage.from(legacyMessage.index(), new LinkedHashMap<>(legacyMessage.message())))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // use fieldsInOrder from undecoratedChunk, because the order can get mixed up when decorators are applied
        return SimpleMessageChunk.from(fieldsInOrder, messages);
    }

    private SearchResponse legacySearchResponseFrom(SimpleMessageChunk chunk, ExportMessagesCommand command) {
        final List<ResultMessageSummary> legacyMessages = legacyMessagesFrom(chunk);

        String queryString = command.queryString().queryString();
        TimeRange timeRange = command.timeRange();
        return SearchResponse.create(
                queryString,
                queryString,
                Collections.emptySet(),
                legacyMessages,
                chunk.fieldsInOrder(),
                -1,
                -1,
                timeRange.getFrom(),
                timeRange.getTo()
        );
    }

    private List<ResultMessageSummary> legacyMessagesFrom(SimpleMessageChunk chunk) {
        return chunk.messages().stream()
                .map(simpleMessage -> ResultMessageSummary.create(ImmutableMultimap.of(), simpleMessage.fields(), simpleMessage.index()))
                .collect(Collectors.toList());
    }
}
