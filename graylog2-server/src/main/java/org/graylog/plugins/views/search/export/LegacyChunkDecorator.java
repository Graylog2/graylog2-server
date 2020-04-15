package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableMultimap;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;

public class LegacyChunkDecorator implements ChunkDecorator {
    private final Map<String, SearchResponseDecorator.Factory> searchResponseDecorators;
    private final DecoratorProcessor decoratorProcessor;

    @Inject
    public LegacyChunkDecorator(Map<String, SearchResponseDecorator.Factory> searchResponseDecorators, DecoratorProcessor decoratorProcessor) {
        this.searchResponseDecorators = searchResponseDecorators;
        this.decoratorProcessor = decoratorProcessor;
    }

    @Override
    public SimpleMessageChunk decorate(SimpleMessageChunk chunk, MessageList ml) {

        SearchResponse sr = legacySearchResponseFrom(chunk);

        SearchResponse searchResponse = decorateSearchResponse(sr, ml);

        return simpleMessageChunkFrom(searchResponse, chunk.isFirstChunk());
    }

    private SimpleMessageChunk simpleMessageChunkFrom(SearchResponse searchResponse, boolean isFirstChunk) {
        LinkedHashSet<SimpleMessage> messages = searchResponse.messages().stream()
                .map(legacyMessage -> SimpleMessage.from(new LinkedHashMap<String, Object>(legacyMessage.message())))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return SimpleMessageChunk.from(new LinkedHashSet<>(searchResponse.fields()), messages)
                .toBuilder().isFirstChunk(isFirstChunk).build();
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

    private SearchResponse decorateSearchResponse(SearchResponse searchResponse, MessageList ml) {
        if (ml.decorators().isEmpty()) {
            return searchResponse;
        }
        final List<SearchResponseDecorator> searchResponseDecorators = ml.decorators()
                .stream()
                .sorted(Comparator.comparing(Decorator::order))
                .map(decorator -> this.searchResponseDecorators.get(decorator.type()).create(decorator))
                .collect(Collectors.toList());
        return decoratorProcessor.decorate(searchResponse, searchResponseDecorators);
    }
}
