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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public class QueryStringDecorators implements QueryStringDecorator {
    private final Set<QueryStringDecorator> queryDecorators;

    public static class Fake extends QueryStringDecorators {
        public Fake() {
            super(Collections.emptySet());
        }
    }

    @Inject
    public QueryStringDecorators(Set<QueryStringDecorator> queryDecorators) {
        this.queryDecorators = queryDecorators;
    }

    @Override
    public String decorate(String queryString, SearchJob job, Query query, Set<QueryResult> results) {
        return this.queryDecorators.isEmpty() ? queryString : this.queryDecorators.stream()
                .reduce(queryString, (prev, decorator) -> decorator.decorate(prev, job, query, results), String::concat);
    }
}
