package org.graylog.plugins.enterprise.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import one.util.streamex.StreamEx;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryMetadata;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.params.QueryReferenceBinding;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.graylog.plugins.enterprise.search.util.GraphToDot.toDot;

public class QueryPlan {
    private final QueryEngine queryEngine;
    private final SearchJob searchJob;
    private ImmutableGraph<Query> plan;
    private final Query rootNode = Query.emptyRoot();

    public QueryPlan(QueryEngine queryEngine, SearchJob searchJob) {
        this.queryEngine = queryEngine;
        this.searchJob = searchJob;
        plan();
    }

    protected CompletableFuture<Void> plan() {
        final MutableGraph<Query> planGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
        planGraph.addNode(rootNode);

        // topologically order search queries based on their parameter usage
        // unconnected queries will degenerate into a simple top-level set
        final Search search = searchJob.getSearch();
        for (Query currentQuery : search.queries()) {
            // if this query does not reference any others explicitly, we will use the rootNode as its source
            final QueryMetadata queryMetadata = queryEngine.parse(search, currentQuery);
            final Stream<Parameter> parameterStream = queryMetadata.usedParameterNames().stream()
                    .map(search::getParameter)
                    .filter(Optional::isPresent)
                    .map(Optional::get);
            final Set<String> referencedQueryIds = StreamEx.of(parameterStream)
                    .map(Parameter::binding)
                    .filter(QueryReferenceBinding.class::isInstance)
                    .map(binding -> (QueryReferenceBinding) binding)
                    .map(QueryReferenceBinding::queryId)
                    .toSetAndThen(ids -> {
                        if (ids.isEmpty()) {
                            ids.add(rootNode.id());
                        }
                        return ids;
                    });

            // add edges from each source node to this one, typically this will be only a single source
            for (String sourceQueryId : referencedQueryIds) {
                final Query sourceQuery = search.getQuery(sourceQueryId).orElse(rootNode);
                planGraph.addNode(sourceQuery);
                planGraph.addNode(currentQuery);
                planGraph.putEdge(sourceQuery, currentQuery);
            }
        }
        this.plan = ImmutableGraph.copyOf(planGraph);
        return CompletableFuture.allOf();
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("searchJob", searchJob)
                .add("plan", plan)
                .add("dot", toDot(plan, Query::id, query -> firstNonNull(query.query(), "Root").toString()))
                .toString();
    }

    public Stream<Query> queryStream() {
        // filter out the root node from the stream of plan elements to visit, because it doesn't do anything useful
        // other than giving us a start point into the graph
        return breadthFirst().filter(element -> !rootNode.equals(element));
    }

    public Stream<Query> breadthFirst() {
        return StreamSupport.stream(Traverser.forGraph(plan).breadthFirst(rootNode).spliterator(), false);
    }

    public ImmutableList<Query> queries() {
        return queryStream().collect(toImmutableList());
    }

    public Set<Query> predecessors(Query query) {
        return plan.predecessors(query);
    }

    public Set<Query> successors(Query query) {
        return plan.successors(query);
    }
}
