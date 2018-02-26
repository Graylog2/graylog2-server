package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;

/**
 *
 * @param <S> the SearchType implementation this handler deals with
 * @param <Q> the backend-specific query builder
 * @param <R> the backend-specific result type
 */
@SuppressWarnings("unchecked")
public interface SearchTypeHandler<S extends SearchType, Q, R> {

    default void generateQueryPart(SearchJob job, Query query, SearchType searchType, Q queryContext) {
        // We need to typecast manually here, because '? extends SearchType' and 'SearchType' are never compatible
        // and thus the compiler won't accept the types at their call sites
        // This allows us to get proper types in the implementing classes instead of having to cast there.
        doGenerateQueryPart(job, query, (S) searchType, queryContext);
    }

    void doGenerateQueryPart(SearchJob job, Query query, S searchType, Q queryContext);

    default SearchType.Result extractResult(SearchJob job, Query query, SearchType searchType, R queryResult, Q queryContext) {
        // see above for the reason for typecasting
        return doExtractResult(job, query, (S) searchType, queryResult, queryContext);
    }

    SearchType.Result doExtractResult(SearchJob job, Query query, S searchType, R queryResult, Q queryContext);
}
