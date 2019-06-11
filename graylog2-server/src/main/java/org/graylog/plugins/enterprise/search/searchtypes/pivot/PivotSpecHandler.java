package org.graylog.plugins.enterprise.search.searchtypes.pivot;

import org.graylog.plugins.enterprise.search.engine.GeneratedQueryContext;
import org.graylog.plugins.enterprise.search.engine.SearchTypeHandler;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Implementations of this class contribute handlers for buckets and series to concrete implementations of {@link Pivot the pivot search type}.
 * @param <SPEC_TYPE> the type of bucket or series spec this handler deals with
 * @param <AGGREGATION_BUILDER> implementation specific type for building up the aggregation when generating a backend query
 * @param <QUERY_RESULT> the backend specific type holding the overall result from the backend
 * @param <AGGREGATION_RESULT> the backend specific type holding the partial result for the generated aggregation
 * @param <SEARCHTYPE_HANDLER> the backend specific type of the surrounding pivot search type handler
 * @param <QUERY_CONTEXT> an opaque context object to pass around information between query generation and result handling
 */
public interface PivotSpecHandler<SPEC_TYPE extends PivotSpec, AGGREGATION_BUILDER, QUERY_RESULT, AGGREGATION_RESULT, SEARCHTYPE_HANDLER, QUERY_CONTEXT> {

    @SuppressWarnings("unchecked")
    @Nonnull
    default Optional<AGGREGATION_BUILDER> createAggregation(String name, PivotSpec pivotSpec, SearchTypeHandler searchTypeHandler, GeneratedQueryContext queryContext) {
        return doCreateAggregation(name, (SPEC_TYPE) pivotSpec, (SEARCHTYPE_HANDLER) searchTypeHandler, (QUERY_CONTEXT) queryContext);
    }

    @Nonnull
    Optional<AGGREGATION_BUILDER> doCreateAggregation(String name, SPEC_TYPE pivotSpec, SEARCHTYPE_HANDLER searchTypeHandler, QUERY_CONTEXT queryContext);

    @SuppressWarnings("unchecked")
    default Object handleResult(PivotSpec pivotSpec, Object queryResult, Object aggregationResult, SearchTypeHandler searchTypeHandler, GeneratedQueryContext queryContext) {
        return doHandleResult((SPEC_TYPE) pivotSpec, (QUERY_RESULT) queryResult, (AGGREGATION_RESULT) aggregationResult, (SEARCHTYPE_HANDLER) searchTypeHandler, (QUERY_CONTEXT) queryContext);
    }

    Object doHandleResult(SPEC_TYPE pivotSpec, QUERY_RESULT queryResult, AGGREGATION_RESULT result, SEARCHTYPE_HANDLER searchTypeHandler, QUERY_CONTEXT queryContext);

}
