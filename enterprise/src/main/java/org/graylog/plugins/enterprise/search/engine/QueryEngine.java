package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryInfo;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class QueryEngine {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEngine.class);
    private static final Query EMPTY_ROOT = Query.emptyRoot();

    private final Map<String, QueryBackend> queryBackends;

    @Inject
    public QueryEngine(Map<String, QueryBackend> queryBackends) {
        this.queryBackends = queryBackends;
    }

    public QueryInfo parse(Query query) {
        final BackendQuery backendQuery = query.query();
        if (backendQuery == null) {
            throw new NullPointerException("query cannot be empty");
        }
        final QueryBackend queryBackend = queryBackends.get(backendQuery.type());
        return queryBackend.parse(query);
    }

    public SearchJob execute(SearchJob searchJob) {
        QueryPlan plan = new QueryPlan(searchJob);

        // generate backend query for each plan element
        plan.forEach(plannedQuery -> {
            final Query query = plannedQuery.getQuery();
            if (query.equals(EMPTY_ROOT)) {
                // skip empty root query
                return;
            }
            final BackendQuery backendQuery = query.query();
            if (backendQuery == null) {
                throw new NullPointerException("query cannot be empty");
            }
            final QueryBackend queryBackend = queryBackends.get(backendQuery.type());
            if (queryBackend == null) {
                throw new IllegalStateException("Unknown query backend " + backendQuery.type() + ", cannot execute query");
            }
            final Object generatedQuery = queryBackend.generate(searchJob, query);
            plannedQuery.setGeneratedQuery(generatedQuery);
            LOG.warn("Generated query: {}", generatedQuery.toString());

        });

        LOG.warn("Search job {} executing with plan {}", searchJob.getId(), plan);
        plan.execute(queryBackends);
        return searchJob;
    }
}
