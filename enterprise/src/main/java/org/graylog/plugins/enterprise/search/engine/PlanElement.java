package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * PlanElement is an annotated {@link Query} which is being used during plan execution.
 */
public class PlanElement {
    private static final PlanElement INSTANCE = new PlanElement(Query.emptyRoot());

    private final Query query;

    @Nullable
    private Object generatedQuery;

    private CompletableFuture<QueryResult> futureResult;

    public PlanElement(Query query) {
        this(query, null);
    }

    public PlanElement(Query query, Object generatedQuery) {
        this.query = query;
        this.generatedQuery = generatedQuery;
    }

    public static PlanElement rootElement() {
        return INSTANCE;
    }

    public String getId() {
        return query.id();
    }

    public Query getQuery() {
        return query;
    }

    public void setGeneratedQuery(Object generatedQuery) {
        this.generatedQuery = generatedQuery;
    }

    @Nullable
    public Object getGeneratedQuery() {
        return generatedQuery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanElement that = (PlanElement) o;
        return Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query);
    }

    public CompletableFuture<QueryResult> getFutureResult() {
        return futureResult;
    }

    public void setFutureResult(CompletableFuture<QueryResult> futureResult) {
        this.futureResult = futureResult;
    }
}
