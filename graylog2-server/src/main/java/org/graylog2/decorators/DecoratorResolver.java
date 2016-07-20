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
import org.graylog2.plugin.decorators.SearchResponseDecorator;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class DecoratorResolver {
    private final DecoratorService decoratorService;
    private final Map<String, SearchResponseDecorator.Factory> searchResponseDecoratorsMap;

    @Inject
    public DecoratorResolver(DecoratorService decoratorService,
                             Map<String, SearchResponseDecorator.Factory> searchResponseDecorators) {
        this.decoratorService = decoratorService;
        this.searchResponseDecoratorsMap = searchResponseDecorators;
    }

    public List<SearchResponseDecorator> searchResponseDecoratorsForStream(String streamId) {
        return this.decoratorService.findForStream(streamId).stream()
            .map(this::instantiateSearchResponseDecorator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<SearchResponseDecorator> searchResponseDecoratorsForGlobal() {
        return this.decoratorService.findForGlobal().stream()
            .map(this::instantiateSearchResponseDecorator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Nullable
    private SearchResponseDecorator instantiateSearchResponseDecorator(Decorator decorator) {
        final SearchResponseDecorator.Factory factory = this.searchResponseDecoratorsMap.get(decorator.type());
        if (factory != null) {
            return factory.create(decorator);
        }
        return null;
    }
}
