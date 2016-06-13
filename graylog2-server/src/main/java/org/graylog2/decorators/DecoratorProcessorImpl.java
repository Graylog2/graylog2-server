package org.graylog2.decorators;

import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DecoratorProcessorImpl implements DecoratorProcessor {
    private final DecoratorResolver decoratorResolver;

    @Inject
    public DecoratorProcessorImpl(DecoratorResolver decoratorResolver) {
        this.decoratorResolver = decoratorResolver;
    }

    @Override
    public List<ResultMessage> decorate(List<ResultMessage> messages) {
        final Optional<MessageDecorator> metaDecorator = decoratorResolver.messageDecoratorsForGlobal().stream()
            .reduce((f, g) -> (v) -> f.apply(g.apply(v)));
        if (metaDecorator.isPresent()) {
            return messages.stream().map(metaDecorator.get()).collect(Collectors.toList());
        }
        return messages;
    }

    @Override
    public SearchResponse decorate(SearchResponse searchResponse) {
        final Optional<SearchResponseDecorator> metaDecorator = decoratorResolver.searchResponseDecoratorsForGlobal().stream()
            .reduce((f, g) -> (v) -> f.apply(g.apply(v)));
        if (metaDecorator.isPresent()) {
            return metaDecorator.get().apply(searchResponse);
        }

        return searchResponse;
    }
}
