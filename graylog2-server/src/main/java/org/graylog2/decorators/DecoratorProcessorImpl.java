package org.graylog2.decorators;

import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class DecoratorProcessorImpl implements DecoratorProcessor {
    private final DecoratorResolver decoratorResolver;

    @Inject
    public DecoratorProcessorImpl(DecoratorResolver decoratorResolver) {
        this.decoratorResolver = decoratorResolver;
    }

    @Override
    public List<ResultMessage> decorate(List<ResultMessage> messages, Optional<String> streamId) {
        final List<MessageDecorator> messageDecorators = streamId.isPresent() ?
            decoratorResolver.messageDecoratorsForStream(streamId.get()) : decoratorResolver.messageDecoratorsForGlobal();
        final Optional<MessageDecorator> metaDecorator = messageDecorators.stream()
            .reduce((f, g) -> (v) -> f.apply(g.apply(v)));
        if (metaDecorator.isPresent()) {
            return metaDecorator.get().apply(messages);
        }
        return messages;
    }

    @Override
    public SearchResponse decorate(SearchResponse searchResponse, Optional<String> streamId) {
        final List<SearchResponseDecorator> searchResponseDecorators = streamId.isPresent() ?
            decoratorResolver.searchResponseDecoratorsForStream(streamId.get()) : decoratorResolver.searchResponseDecoratorsForGlobal();
        final Optional<SearchResponseDecorator> metaDecorator = searchResponseDecorators.stream()
            .reduce((f, g) -> (v) -> f.apply(g.apply(v)));
        if (metaDecorator.isPresent()) {
            return metaDecorator.get().apply(searchResponse);
        }

        return searchResponse;
    }
}
