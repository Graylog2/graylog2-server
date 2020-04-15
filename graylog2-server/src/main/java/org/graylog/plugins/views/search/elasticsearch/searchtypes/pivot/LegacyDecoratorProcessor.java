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
package org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot;

import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LegacyDecoratorProcessor {
    private final DecoratorProcessor decoratorProcessor;
    private final Map<String, SearchResponseDecorator.Factory> searchResponseDecorators;

    @Inject
    public LegacyDecoratorProcessor(DecoratorProcessor decoratorProcessor,
                                    Map<String, SearchResponseDecorator.Factory> searchResponseDecorators) {
        this.decoratorProcessor = decoratorProcessor;
        this.searchResponseDecorators = searchResponseDecorators;
    }

    public SearchResponse decorateSearchResponse(SearchResponse searchResponse, List<Decorator> decorators) {
        if (decorators.isEmpty()) {
            return searchResponse;
        }
        final List<SearchResponseDecorator> searchResponseDecorators = decorators
                .stream()
                .sorted(Comparator.comparing(Decorator::order))
                .map(decorator -> this.searchResponseDecorators.get(decorator.type()).create(decorator))
                .collect(Collectors.toList());
        return decoratorProcessor.decorate(searchResponse, searchResponseDecorators);
    }

    public static class Fake extends LegacyDecoratorProcessor {
        public Fake() {
            super(null, null);
        }

        @Override
        public SearchResponse decorateSearchResponse(SearchResponse searchResponse, List<Decorator> decorators) {
            return searchResponse;
        }
    }
}
