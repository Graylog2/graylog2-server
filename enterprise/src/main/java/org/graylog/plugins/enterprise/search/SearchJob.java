package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.Maps;
import one.util.streamex.EntryStream;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@JsonAutoDetect
@JsonPropertyOrder({"execution", "results"}) // execution must come before results, as it signals the overall "done" state
public class SearchJob {

    @JsonProperty
    private final String id;

    @JsonIgnore
    private final Search search;

    @JsonProperty("execution")
    private CompletableFuture<Void> resultFuture;

    private Map<String, CompletableFuture<QueryResult>> queryResults = Maps.newHashMap();

    public SearchJob(String id, Search search) {
        this.id = id;
        this.search = search;
    }

    public String getId() {
        return id;
    }

    public Search getSearch() {
        return search;
    }

    @JsonProperty("search_id")
    public String getQueryId() { return search.id(); }

    public CompletableFuture<Void> getResultFuture() {
        return resultFuture;
    }

    public void setResultFuture(CompletableFuture<Void> resultFuture) {
        this.resultFuture = resultFuture;
    }

    public void addQueryResultFuture(String queryId, CompletableFuture<QueryResult> resultFuture) {
        queryResults.put(queryId, resultFuture);
    }

    @JsonProperty("results")
    public Map<String, QueryResult> results() {
        final Map<String, QueryResult> results = EntryStream.of(queryResults)
                .mapValues(future -> future.getNow(QueryResult.emptyResult()))
                .toMap();

        return results;
    }
}
