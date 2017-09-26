package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.CompletableFuture;

@JsonAutoDetect
public class QueryJob {

    @JsonProperty
    private final String id;

    @JsonIgnore
    private final Query query;

    @JsonProperty
    private CompletableFuture<QueryResult> resultFuture;

    public QueryJob(String id, Query query) {
        this.id = id;
        this.query = query;
    }


    public String getId() {
        return id;
    }

    public Query getQuery() {
        return query;
    }

    public CompletableFuture<QueryResult> getResultFuture() {
        return resultFuture;
    }

    public void setResultFuture(CompletableFuture<QueryResult> resultFuture) {
        this.resultFuture = resultFuture;
    }
}
