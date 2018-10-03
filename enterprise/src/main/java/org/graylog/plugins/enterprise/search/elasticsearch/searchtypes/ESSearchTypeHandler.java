package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.engine.SearchTypeHandler;

/**
 * Signature of search type handlers the elasticsearch backend takes.
 * All of these take a {@link ESGeneratedQueryContext} as input.
 *
 * @param <S> the {@link org.graylog.plugins.enterprise.search.SearchType SearchType} this handler deals with
 */
public interface ESSearchTypeHandler<S extends SearchType> extends SearchTypeHandler<S, ESGeneratedQueryContext, SearchResult> {
    @Override
    default SearchType.Result doExtractResultImpl(SearchJob job, Query query, S searchType, SearchResult queryResult, ESGeneratedQueryContext queryContext) {
        // if the search type was filtered, extract the sub-aggregation before passing it to the handler
        // this way we don't have to duplicate this step everywhere
        MetricAggregation aggregations = queryResult.getAggregations();
        return doExtractResult(job, query, searchType, queryResult, aggregations, queryContext);
    }

    SearchType.Result doExtractResult(SearchJob job, Query query, S searchType, SearchResult queryResult, MetricAggregation aggregations, ESGeneratedQueryContext queryContext);
}
