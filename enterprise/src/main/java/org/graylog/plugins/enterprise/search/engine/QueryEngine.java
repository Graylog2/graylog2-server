package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QueryEngine {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEngine.class);

    private final Map<String, QueryBackend> queryBackends;

    public QueryEngine(Map<String, QueryBackend> queryBackends) {
        this.queryBackends = queryBackends;
    }

    public CompletableFuture<QueryResult> execute(Query query) {
        return CompletableFuture.supplyAsync(() -> doExecute(query));
    }

    private QueryResult doExecute(Query query) {
        // TODO validate query

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
        final QueryResult queryResult = queryBackend.run(query, generatedQuery);

        return new QueryResult(query);
    }
}
