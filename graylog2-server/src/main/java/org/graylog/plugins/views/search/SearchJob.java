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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import one.util.streamex.EntryStream;
import org.graylog.plugins.views.search.errors.SearchError;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@JsonAutoDetect
// execution must come before results, as it signals the overall "done" state
@JsonPropertyOrder({"execution", "results"})
public class SearchJob implements ParameterProvider {
    private final String id;

    private final Search search;

    private final String owner;

    private CompletableFuture<Void> resultFuture;

    private final Map<String, CompletableFuture<QueryResult>> queryResults = Maps.newHashMap();

    private Set<SearchError> errors = Sets.newHashSet();

    public SearchJob(String id, Search search, String owner) {
        this.id = id;
        this.search = search;
        this.owner = owner;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonIgnore
    public Search getSearch() {
        return search;
    }

    @JsonProperty("search_id")
    public String getSearchId() {
        return search.id();
    }

    @JsonProperty
    public String getOwner() {
        return owner;
    }

    @JsonProperty("errors")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Set<SearchError> getErrors() {
        return errors;
    }

    @JsonIgnore
    public CompletableFuture<Void> getResultFuture() {
        return resultFuture;
    }

    public void addQueryResultFuture(String queryId, CompletableFuture<QueryResult> resultFuture) {
        queryResults.put(queryId, resultFuture);
    }

    @JsonProperty("results")
    public Map<String, QueryResult> results() {
        return EntryStream.of(queryResults)
                .mapValues(future -> future.getNow(QueryResult.incomplete()))
                .filterKeys(queryId -> !queryId.isEmpty()) // the root query result is meaningless, so we don't include it here
                .filterValues(r -> (r.state() == QueryResult.State.COMPLETED) || (r.state() == QueryResult.State.FAILED))
                .toMap();
    }

    @JsonProperty("execution")
    public ExecutionInfo execution() {
        final boolean isDone = resultFuture != null && resultFuture.isDone();
        final boolean isCancelled = resultFuture != null && resultFuture.isCancelled();
        return new ExecutionInfo(isDone, isCancelled, !errors.isEmpty());
    }

    public CompletableFuture<QueryResult> getQueryResultFuture(String queryId) {
        return queryResults.get(queryId);
    }

    @JsonIgnore
    public SearchJob seal() {
        // for each QueryResult future, add an exception handler so we at least get a FAILED result instead of the generic exception for everything
        this.resultFuture = CompletableFuture.allOf(queryResults.values().toArray(new CompletableFuture[0]));
        return this;
    }

    public void addError(SearchError t) {
        errors.add(t);
    }

    @Override
    public Optional<Parameter> getParameter(String name) {
        return getSearch().getParameter(name);
    }

    public static class ExecutionInfo {
        @JsonProperty("done")
        public final boolean done;
        @JsonProperty("cancelled")
        public final boolean cancelled;
        @JsonProperty("completed_exceptionally")
        public final boolean hasErrors;

        ExecutionInfo(boolean done, boolean cancelled, boolean hasErrors) {
            this.done = done;
            this.cancelled = cancelled;
            this.hasErrors = hasErrors;
        }
    }
}
