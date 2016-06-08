package org.graylog2.decorators;

import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DecoratorProcessorImpl implements DecoratorProcessor {
    private final Set<MessageDecorator> messageDecoratorSet;
    private final Set<SearchResponseDecorator> searchResponseDecorators;
    private final DecoratorService decoratorService;
    private final List<Decorator> decorators;

    @Inject
    public DecoratorProcessorImpl(Set<MessageDecorator> messageDecoratorSet,
                                  Set<SearchResponseDecorator> searchResponseDecorators,
                                  DecoratorService decoratorService) {
        this.messageDecoratorSet = messageDecoratorSet;
        this.searchResponseDecorators = searchResponseDecorators;
        this.decoratorService = decoratorService;
        this.decorators = decoratorService.findAll();
    }

    @Override
    public List<ResultMessage> decorate(List<ResultMessage> messages) {
        final Optional<MessageDecorator> metaDecorator = this.messageDecoratorSet.stream().reduce((f, g) -> (v) -> f.apply(g.apply(v)));
        if (metaDecorator.isPresent()) {
            return messages.stream().map(metaDecorator.get()).collect(Collectors.toList());
        }
        return messages;
    }

    @Override
    public SearchResponse decorate(SearchResponse searchResponse) {
        final Optional<SearchResponseDecorator> metaDecorator = this.searchResponseDecorators.stream().reduce((f, g) -> (v) -> f.apply(g.apply(v)));
        if (metaDecorator.isPresent()) {
            return metaDecorator.get().apply(searchResponse);
        }

        return searchResponse;
    }
}
