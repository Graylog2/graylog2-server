/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableMultimap;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.LegacyDecoratorProcessor;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;

public class LegacyChunkDecorator implements ChunkDecorator {

    private final LegacyDecoratorProcessor decoratorProcessor;

    @Inject
    public LegacyChunkDecorator(LegacyDecoratorProcessor decoratorProcessor) {
        this.decoratorProcessor = decoratorProcessor;
    }

    @Override
    public SimpleMessageChunk decorate(SimpleMessageChunk undecoratedChunk, MessageList messageList) {

        SearchResponse undecoratedLegacyResponse = legacySearchResponseFrom(undecoratedChunk);

        SearchResponse decoratedLegacyResponse = decoratorProcessor.decorateSearchResponse(undecoratedLegacyResponse, messageList.decorators());

        SimpleMessageChunk decoratedChunk = simpleMessageChunkFrom(decoratedLegacyResponse);

        return decoratedChunk.toBuilder().isFirstChunk(undecoratedChunk.isFirstChunk()).build();
    }

    private SimpleMessageChunk simpleMessageChunkFrom(SearchResponse searchResponse) {
        LinkedHashSet<SimpleMessage> messages = searchResponse.messages().stream()
                .map(legacyMessage -> SimpleMessage.from(new LinkedHashMap<String, Object>(legacyMessage.message())))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return SimpleMessageChunk.from(new LinkedHashSet<>(searchResponse.fields()), messages);
    }

    private SearchResponse legacySearchResponseFrom(SimpleMessageChunk chunk) {
        final List<ResultMessageSummary> messages = chunk.messages().stream()
                .map(simpleMessage -> ResultMessageSummary.create(ImmutableMultimap.of(), simpleMessage.fields(), ""))
                .collect(Collectors.toList());

        // we are only interested in the decorated message fields.
        //
        return SearchResponse.create(
                "",
                "",
                Collections.emptySet(),
                messages,
                Collections.emptySet(),
                0,
                0,
                now(),
                now()
        );
    }
}
