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

import com.google.inject.Singleton;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class DecoratorResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DecoratorResolver.class);

    private final DecoratorService decoratorService;
    private final Map<String, SearchResponseDecorator.Factory> searchResponseDecoratorsMap;

    @Inject
    public DecoratorResolver(DecoratorService decoratorService,
                             Map<String, SearchResponseDecorator.Factory> searchResponseDecorators) {
        this.decoratorService = decoratorService;
        this.searchResponseDecoratorsMap = searchResponseDecorators;
    }

    public List<SearchResponseDecorator> searchResponseDecoratorsForStream(String streamId) {
        return this.decoratorService.findForStream(streamId)
            .stream()
            .sorted()
            .map(this::instantiateSearchResponseDecorator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<SearchResponseDecorator> searchResponseDecoratorsForGlobal() {
        return this.decoratorService.findForGlobal()
            .stream()
            .sorted()
            .map(this::instantiateSearchResponseDecorator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Nullable
    private SearchResponseDecorator instantiateSearchResponseDecorator(Decorator decorator) {
        final SearchResponseDecorator.Factory factory = this.searchResponseDecoratorsMap.get(decorator.type());
        if (factory != null) {
            try {
                return factory.create(decorator);
            } catch(Exception e) {
                LOG.error("Unable to create <{}> decorator", factory.getDescriptor().getName(), e);
            }
        }
        return null;
    }
}
