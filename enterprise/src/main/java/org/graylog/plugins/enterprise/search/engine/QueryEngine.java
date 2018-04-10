package org.graylog.plugins.enterprise.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.util.streamex.StreamEx;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryMetadata;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Singleton
public class QueryEngine {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEngine.class);

    private final Map<String, QueryBackend<? extends GeneratedQueryContext>> queryBackends;

    // TODO proper thread pool with tunable settings
    private final Executor queryPool = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("query-engine-%d").build());

    @Inject
    public QueryEngine(Map<String, QueryBackend<? extends GeneratedQueryContext>> queryBackends) {
        this.queryBackends = queryBackends;
    }

    private static <T> CompletableFuture<Set<T>> allOfResults(Set<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toSet())
                );
    }

    public QueryMetadata parse(Search search, Query query) {
        final BackendQuery backendQuery = query.query();
        final QueryBackend queryBackend = queryBackends.get(backendQuery.type());
        return queryBackend.parse(search.parameters(), query);
    }

    public SearchJob execute(SearchJob searchJob) {
        final QueryPlan plan = new QueryPlan(this, searchJob);

        final ImmutableList<Query> queries = plan.queries();
        queries.forEach(query -> {
            final CompletableFuture<QueryResult> resultFuture = new CompletableFuture<>();
            searchJob.addQueryResultFuture(query.id(), resultFuture);
        });
        // the root is always complete
        searchJob.addQueryResultFuture("", CompletableFuture.completedFuture(QueryResult.emptyResult()));


        plan.breadthFirst().forEachOrdered(query -> {
            final Set<Query> predecessors = plan.predecessors(query);
            LOG.warn("[{}] Processing query, requires {} results, has {} subqueries",
                    defaultIfEmpty(query.id(), "root"), predecessors.size(), plan.successors(query).size());

            // if the query has an immediate result, we don't need to generate anything. this is currently only true for the dummy root query
            final CompletableFuture<QueryResult> queryResultFuture = searchJob.getQueryResultFuture(query.id());
            if (!queryResultFuture.isDone()) {
                final QueryBackend<? extends GeneratedQueryContext> backend = getQueryBackend(query);
                LOG.warn("[{}] Using {} to generate query", query.id(), backend);

                LOG.warn("[{}] Waiting for results: {}", query.id(), predecessors);
                // gather all required results to be able to execute the current query
                allOfResults(predecessors.stream().map(Query::id).map(searchJob::getQueryResultFuture).collect(Collectors.toSet()))
                        .thenAccept(results -> {
                            LOG.debug("[{}] Preparing query execution with results of queries: ({})",
                                    query.id(), StreamEx.of(results.stream()).map(QueryResult::query).map(Query::id).joining());

                            // with all the results done, we can execute the current query and eventually complete our own result
                            final GeneratedQueryContext generatedQueryContext = backend.generate(searchJob, query, results);
                            LOG.trace("[{}] Generated query: {}", query.id(), generatedQueryContext);
                            queryPool.execute(() -> {
                                try {
                                    LOG.debug("[{}] Running query on backend {}", query.id(), backend);
                                    final QueryResult result = backend.run(searchJob, query, generatedQueryContext, results);
                                    LOG.debug("[{}] Completing query {}", query.id(), queries);
                                    queryResultFuture.complete(result);
                                    LOG.debug("[{}] Query returned {}", query.id(), result);
                                } catch (Exception e) {
                                    queryResultFuture.completeExceptionally(e);
                                    LOG.warn("[{}] Query failed: {}", query.id(), ExceptionUtils.getRootCauseMessage(e));
                                }
                            });
                        }).join();
            } else {
                LOG.warn("[{}] Not generating query for query {}", defaultIfEmpty(query.id(), "root"), queries);
            }
        });

        LOG.warn("Search job {} executing with plan {}", searchJob.getId(), plan);
        return searchJob.seal();
    }

    private QueryBackend<? extends GeneratedQueryContext> getQueryBackend(Query query) {
        final BackendQuery backendQuery = query.query();
        final QueryBackend<? extends GeneratedQueryContext> queryBackend = queryBackends.get(backendQuery.type());
        if (queryBackend == null) {
            throw new IllegalStateException("[{" + query.id() + "}] Unknown query backend " + backendQuery.type() + ", cannot execute query");
        }
        return queryBackend;
    }
}
