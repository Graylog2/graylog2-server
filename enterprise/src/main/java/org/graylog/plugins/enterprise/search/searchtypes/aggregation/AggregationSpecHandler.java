package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import org.graylog.plugins.enterprise.search.engine.GeneratedQueryContext;
import org.graylog.plugins.enterprise.search.engine.SearchTypeHandler;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * This interface defines the plugin point for handling group aggregations in backends.
 * <p>
 * Implementations of this define how group aggregations are generated into queries and how their results are parsed.
 *
 * @param <SPEC_TYPE>           the type of the concrete subclass of a {@link GroupSpec}
 * @param <AGGREGATION_BUILDER> the type of the query or aggregation builder
 * @param <AGGREGATION_RESULT>        the type of the query result for the backend
 */
@SuppressWarnings("unchecked")
public interface AggregationSpecHandler<SPEC_TYPE extends AggregationSpec, AGGREGATION_BUILDER, QUERY_RESULT, AGGREGATION_RESULT, SEARCHTYPE_HANDLER, QUERY_CONTEXT> {

    @Nonnull
    default Optional<AGGREGATION_BUILDER> createAggregation(String name, AggregationSpec aggregationSpec, SearchTypeHandler searchTypeHandler, GeneratedQueryContext queryContext) {
        return doCreateAggregation(name, (SPEC_TYPE) aggregationSpec, (SEARCHTYPE_HANDLER) searchTypeHandler, (QUERY_CONTEXT) queryContext);
    }

    @Nonnull
    Optional<AGGREGATION_BUILDER> doCreateAggregation(String name, SPEC_TYPE aggregationSpec, SEARCHTYPE_HANDLER searchTypeHandler, QUERY_CONTEXT queryContext);

    default Object handleResult(AggregationSpec aggregationSpec, Object queryResult, Object aggregationResult, SearchTypeHandler searchTypeHandler, GeneratedQueryContext queryContext) {
        return doHandleResult((SPEC_TYPE) aggregationSpec, (QUERY_RESULT) queryResult, (AGGREGATION_RESULT) aggregationResult, (SEARCHTYPE_HANDLER) searchTypeHandler, (QUERY_CONTEXT) queryContext);
    }

    Object doHandleResult(SPEC_TYPE aggregationSpec, QUERY_RESULT queryResult, AGGREGATION_RESULT result, SEARCHTYPE_HANDLER searchTypeHandler, QUERY_CONTEXT queryContext);

}
