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

import com.google.inject.ImplementedBy;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.util.List;
import java.util.Optional;

@ImplementedBy(DecoratorProcessorImpl.class)
public interface DecoratorProcessor {
    SearchResponse decorate(SearchResponse searchResponse, Optional<String> stream);
    SearchResponse decorate(SearchResponse searchResponse, List<SearchResponseDecorator> searchResponseDecorators);

    class Fake implements DecoratorProcessor{
        @Override
        public SearchResponse decorate(SearchResponse searchResponse, Optional<String> stream) {
            return searchResponse;
        }

        @Override
        public SearchResponse decorate(SearchResponse searchResponse, List<SearchResponseDecorator> searchResponseDecorators) {
            return searchResponse;
        }
    }
}
