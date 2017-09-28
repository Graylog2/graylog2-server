package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.SearchType;

/**
 *
 * @param <S> the SearchType implementation this handler deals with
 * @param <Q> the backend-specific query builder
 * @param <R> the backend-specific result type
 */
@SuppressWarnings("unchecked")
public interface SearchTypeHandler<S extends SearchType, Q, R> {

    default void generateQueryPart(SearchType searchType, Q queryBuilder) {
        // unfortunately if we don't typecast here the compiler complains about "capture of ? extends SearchType" and "SearchType" not being compatible.
        // it is probably right, but I can't figure out why now, so I'm taking the short route.
        doGenerateQueryPart((S) searchType, queryBuilder);
    }

    void doGenerateQueryPart(S searchType, Q queryBuilder);

    default SearchType.Result extractResult(SearchType searchType, R queryResult) {
        // see above for the reason for typecasting
        return doExtractResult((S) searchType, queryResult);
    }

    SearchType.Result doExtractResult(S searchType, R queryResult);
}
