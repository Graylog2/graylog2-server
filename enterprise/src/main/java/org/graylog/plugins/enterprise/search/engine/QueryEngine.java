package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryInfo;
import org.graylog.plugins.enterprise.search.QueryJob;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class QueryEngine {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEngine.class);

    private final Map<String, QueryBackend> queryBackends;

    @Inject
    public QueryEngine(Map<String, QueryBackend> queryBackends) {
        this.queryBackends = queryBackends;
    }

    public CompletableFuture<QueryResult> execute(QueryJob job) {
        final CompletableFuture<QueryResult> resultFuture = CompletableFuture.supplyAsync(() -> doExecute(job));
        job.setResultFuture(resultFuture);
        return resultFuture;
    }

    public QueryInfo parse(Query query) {
        final BackendQuery backendQuery = query.query();
        if (backendQuery == null) {
            throw new NullPointerException("query cannot be empty");
        }
        final QueryBackend queryBackend = queryBackends.get(backendQuery.type());
        return queryBackend.parse(query);
    }

    private QueryResult doExecute(QueryJob queryJob) {
        // TODO validate query
        final Query query = queryJob.getQuery();
        final BackendQuery backendQuery = query.query();
        if (backendQuery == null) {
            throw new NullPointerException("query cannot be empty");
        }
        final QueryBackend queryBackend = queryBackends.get(backendQuery.type());
        if (queryBackend == null) {
            throw new IllegalStateException("Unknown query backend " + backendQuery.type() + ", cannot execute query");
        }
        final Object generatedQuery = queryBackend.generate(query);
        LOG.warn("Generated query: {}", generatedQuery.toString());
        final QueryResult queryResult = queryBackend.run(queryJob, generatedQuery);

        return queryResult;
    }
}
