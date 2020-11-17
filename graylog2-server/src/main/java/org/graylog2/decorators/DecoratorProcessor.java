/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
