package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;

import javax.inject.Inject;
import java.util.UUID;

public class QueryStringDecorator {

    private final ESQueryDecorators decorator;

    @Inject
    public QueryStringDecorator(ESQueryDecorators decorator) {
        this.decorator = decorator;
    }

    public String decorateQueryString(String queryString, Search search, Query query) {

        SearchJob jobStub = new SearchJob(UUID.randomUUID().toString(), search, "views backend");

        return decorator.decorate(queryString, jobStub, query, ImmutableSet.of());
    }
}
