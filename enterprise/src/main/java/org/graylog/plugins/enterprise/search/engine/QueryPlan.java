package org.graylog.plugins.enterprise.search.engine;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.plugins.enterprise.search.ParameterBinding;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;
import static org.graylog.plugins.enterprise.search.util.GraphToDot.toDot;

public class QueryPlan {
    private static final Logger LOG = LoggerFactory.getLogger(QueryPlan.class);

    private final SearchJob searchJob;
    private ImmutableGraph<PlanElement> plan;
    private PlanElement rootNode;

    public QueryPlan(SearchJob searchJob) {
        this.searchJob = searchJob;
        plan();
    }

    protected void plan() {
        final MutableGraph<PlanElement> planGraph = GraphBuilder.directed().allowsSelfLoops(false).build();

        this.rootNode = PlanElement.rootElement();
        planGraph.addNode(rootNode);

        // topologically order search queries based on their parameter usage
        // unconnected queries will degenerate into a simple top-level set
        final Search search = searchJob.getSearch();
        search.queries().forEach((Query query) -> {
            // if this query does not reference any others explicitly, we will use the rootNode as its source
            final Set<String> referencedQueries;
            final Map<String, ParameterBinding> parameters = query.parameters();
            if (parameters != null) {
                referencedQueries = parameters.values().stream()
                        .map(ParameterBinding::getReferences)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
            } else {
                referencedQueries = Collections.singleton(rootNode.getId());
            }

            // add edges from each source node to this one, typically this will be only a single source
            referencedQueries.forEach(sourceQueryId -> {
                final PlanElement sourceNode = sourceQueryId.isEmpty() ? rootNode : new PlanElement(search.getQuery(sourceQueryId));
                planGraph.addNode(sourceNode);
                final PlanElement dependentQuery = new PlanElement(query);
                planGraph.addNode(dependentQuery);
                planGraph.putEdge(sourceNode, dependentQuery);
            });
        });
        this.plan = ImmutableGraph.copyOf(planGraph);
    }

    public CompletableFuture<Void> execute(Map<String, QueryBackend> queryBackends) {
        // TODO recurse into graph
        rootNode.setFutureResult(new CompletableFuture<>());
        final Set<CompletableFuture<QueryResult>> futures = plan.successors(rootNode).stream()
                .map(planElement -> futureForPlanElement(queryBackends, planElement, rootNode))
                .collect(Collectors.toSet());
        final CompletableFuture<Void> overallFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}));
        searchJob.setResultFuture(overallFuture);

        rootNode.getFutureResult().complete(QueryResult.emptyResult());
        return overallFuture;
    }

    private CompletableFuture<QueryResult> futureForPlanElement(Map<String, QueryBackend> queryBackends,
                                                                PlanElement current,
                                                                PlanElement predecessor) {
        final Query query = current.getQuery();
        final BackendQuery backendQuery = query.query();
        final QueryBackend queryBackend = queryBackends.get(backendQuery.type());
        if (queryBackend == null) {
            throw new IllegalStateException("Unknown query backend " + backendQuery.type() + ", cannot execute query");
        }
        final CompletableFuture<QueryResult> resultFuture =
                predecessor.getFutureResult()
                        .thenCompose(predecessorResult -> futureQueryRun(current, query, queryBackend, predecessorResult));
        searchJob.addQueryResultFuture(query.id(), resultFuture);
        return resultFuture;
    }

    private CompletableFuture<QueryResult> futureQueryRun(PlanElement current, Query query, QueryBackend queryBackend, QueryResult predecessorResult) {
        return CompletableFuture.supplyAsync(() -> queryBackend.run(searchJob, query, current.getGeneratedQuery(), predecessorResult));
    }


    public void forEach(Consumer<? super PlanElement> action) {
        plan.nodes().forEach(action);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("searchJob", searchJob)
                .add("plan", plan)
                .add("dot", toDot(plan, PlanElement::getId, query -> firstNonNull(query.getQuery().query(), "Root").toString()))
                .toString();
    }
}
