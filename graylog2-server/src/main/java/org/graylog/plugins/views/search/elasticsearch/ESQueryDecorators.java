package org.graylog.plugins.views.search.elasticsearch;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public class ESQueryDecorators implements ESQueryDecorator {
    private final Set<ESQueryDecorator> queryDecorators;

    public static class Fake extends ESQueryDecorators {
        public Fake() {
            super(Collections.emptySet());
        }
    }

    @Inject
    public ESQueryDecorators(Set<ESQueryDecorator> queryDecorators) {
        this.queryDecorators = queryDecorators;
    }

    @Override
    public String decorate(String queryString, SearchJob job, Query query, Set<QueryResult> results) {
        return this.queryDecorators.isEmpty() ? queryString : this.queryDecorators.stream()
                .reduce(queryString, (prev, decorator) -> decorator.decorate(prev, job, query, results), String::concat);
    }
}
