package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.engine.SearchTypeHandler;

/**
 * Signature of search type handlers the elasticsearch backend takes.
 * All of these take a {@link SearchSourceBuilder} as input.
 *
 * @param <S> the {@link org.graylog.plugins.enterprise.search.SearchType SearchType} this handler deals with
 */
public interface ESSearchTypeHandler<S extends SearchType> extends SearchTypeHandler<S, SearchSourceBuilder, SearchResult> {
}
