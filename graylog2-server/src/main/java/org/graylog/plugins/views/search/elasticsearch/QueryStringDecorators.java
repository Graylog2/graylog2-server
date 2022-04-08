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
package org.graylog.plugins.views.search.elasticsearch;

import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.engine.PositionTrackingQuery;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;

import javax.inject.Inject;
import java.util.Set;

public class QueryStringDecorators {
    private final Set<QueryStringDecorator> queryDecorators;

    @Inject
    public QueryStringDecorators(Set<QueryStringDecorator> queryDecorators) {
        this.queryDecorators = queryDecorators;
    }

    public String decorate(String queryString, ParameterProvider job, Query query) {
        return decorateWithPositions(queryString, job, query).getInterpolatedQuery();
    }

    public PositionTrackingQuery decorateWithPositions(String queryString, ParameterProvider job, Query query) {
        return this.queryDecorators.stream()
                .findFirst()
                .map(decorator -> decorator.decorate(queryString, job, query))
                .orElseGet(() -> PositionTrackingQuery.of(queryString));
    }
}
