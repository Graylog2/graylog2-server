package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
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
}
