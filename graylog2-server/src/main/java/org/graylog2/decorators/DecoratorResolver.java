package org.graylog2.decorators;

import com.google.inject.Singleton;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.plugin.streams.Stream;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class DecoratorResolver {
    private final DecoratorService decoratorService;
    private final Map<String, MessageDecorator> messageDecoratorMap;
    private final Map<String, SearchResponseDecorator> searchResponseDecoratorsMap;

    @Inject
    public DecoratorResolver(DecoratorService decoratorService, Set<MessageDecorator> messageDecoratorSet, Set<SearchResponseDecorator> searchResponseDecorators) {
        this.decoratorService = decoratorService;
        this.messageDecoratorMap = messageDecoratorSet.stream().collect(Collectors.toMap((decorator) -> decorator.getClass().getCanonicalName(), Function.identity()));
        this.searchResponseDecoratorsMap = searchResponseDecorators.stream().collect(Collectors.toMap((decorator) -> decorator.getClass().toString(), Function.identity()));
    }

    public List<MessageDecorator> messageDecoratorsForStream(Stream stream) {
        return this.decoratorService.findForStream(stream.getId()).stream()
            .map(decorator -> this.messageDecoratorMap.get(decorator.type()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<MessageDecorator> messageDecoratorsForGlobal() {
        return this.decoratorService.findForGlobal().stream()
            .map(decorator -> this.messageDecoratorMap.get(decorator.type()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<SearchResponseDecorator> searchResponseDecoratorsForStream(Stream stream) {
        return this.decoratorService.findForStream(stream.getId()).stream()
            .map(decorator -> this.searchResponseDecoratorsMap.get(decorator.type()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<SearchResponseDecorator> searchResponseDecoratorsForGlobal() {
        return this.decoratorService.findForGlobal().stream()
            .map(decorator -> this.searchResponseDecoratorsMap.get(decorator.type()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
