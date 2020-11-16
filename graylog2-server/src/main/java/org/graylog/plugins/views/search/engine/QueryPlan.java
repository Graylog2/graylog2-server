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
package org.graylog.plugins.views.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.graylog.plugins.views.search.util.GraphToDot.toDot;

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
            final Set<String> referencedQueries = queryMetadata.referencedQueries().isEmpty()
                    ? Collections.singleton(rootNode.id())
                    : queryMetadata.referencedQueries();

            // add edges from each source node to this one, typically this will be only a single source
            for (String sourceQueryId : referencedQueries) {
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
