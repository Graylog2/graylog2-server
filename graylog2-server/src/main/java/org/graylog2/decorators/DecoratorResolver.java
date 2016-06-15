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
package org.graylog2.decorators;

import com.google.inject.Singleton;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.plugin.decorators.SearchResponseDecorator;

import javax.annotation.Nullable;
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
    private final Map<String, MessageDecorator.Factory> messageDecoratorMap;
    private final Map<String, SearchResponseDecorator> searchResponseDecoratorsMap;

    @Inject
    public DecoratorResolver(DecoratorService decoratorService,
                             Map<String, MessageDecorator.Factory> messageDecorators,
                             Set<SearchResponseDecorator> searchResponseDecorators) {
        this.decoratorService = decoratorService;
        this.messageDecoratorMap = messageDecorators;
        this.searchResponseDecoratorsMap = searchResponseDecorators.stream().collect(Collectors.toMap((decorator) -> decorator.getClass().toString(), Function.identity()));
    }

    public List<MessageDecorator> messageDecoratorsForStream(String streamId) {
        return this.decoratorService.findForStream(streamId).stream()
            .map(this::instantiateMessageDecorator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<MessageDecorator> messageDecoratorsForGlobal() {
        return this.decoratorService.findForGlobal().stream()
            .map(this::instantiateMessageDecorator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<SearchResponseDecorator> searchResponseDecoratorsForStream(String streamId) {
        return this.decoratorService.findForStream(streamId).stream()
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

    @Nullable
    private MessageDecorator instantiateMessageDecorator(Decorator decorator) {
        final MessageDecorator.Factory factory = this.messageDecoratorMap.get(decorator.type());
        if (factory != null) {
            return factory.create(decorator);
        }
        return null;
    }
}
