/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.graylog2.indexer.searches.ScrollCommand;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.Set;

import static org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class SearchRequestFactory {
    private static final Sorting DEFAULT_SORTING = new Sorting("_doc", Sorting.Direction.ASC);
    private final SortOrderMapper sortOrderMapper;
    private final boolean allowHighlighting;
    private final boolean allowLeadingWildcardSearches;

    @Inject
    public SearchRequestFactory(SortOrderMapper sortOrderMapper,
                                @Named("allow_highlighting") boolean allowHighlighting,
                                @Named("allow_leading_wildcard_searches") boolean allowLeadingWildcardSearches) {
        this.sortOrderMapper = sortOrderMapper;
        this.allowHighlighting = allowHighlighting;
        this.allowLeadingWildcardSearches = allowLeadingWildcardSearches;
    }

    public SearchSourceBuilder create(SearchesConfig config) {
        return create(SearchCommand.from(config));
    }

    public SearchSourceBuilder create(ScrollCommand scrollCommand) {
        return create(SearchCommand.from(scrollCommand));
    }

    public SearchSourceBuilder create(SearchCommand searchCommand) {
        final String query = normalizeQuery(searchCommand.query());

        final QueryBuilder queryBuilder = isWildcardQuery(query)
                ? matchAllQuery()
                : queryStringQuery(query).allowLeadingWildcard(allowLeadingWildcardSearches);

        final Optional<BoolQueryBuilder> rangeQueryBuilder = searchCommand.range()
                .map(TimeRangeQueryFactory::create)
                .map(rangeQuery -> boolQuery().must(rangeQuery));
        final Optional<BoolQueryBuilder> filterQueryBuilder = searchCommand.filter()
                .filter(filter -> !isWildcardQuery(filter))
                .map(QueryBuilders::queryStringQuery)
                .map(queryStringQuery -> boolQuery().must(queryStringQuery));

        final BoolQueryBuilder filteredQueryBuilder = boolQuery()
                .must(queryBuilder);
        filterQueryBuilder.ifPresent(filteredQueryBuilder::filter);
        rangeQueryBuilder.ifPresent(filteredQueryBuilder::filter);

        applyStreamsFilter(filteredQueryBuilder, searchCommand);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(filteredQueryBuilder)
                .trackTotalHits(true);

        applyPaginationIfPresent(searchSourceBuilder, searchCommand);

        applySortingIfPresent(searchSourceBuilder, searchCommand);

        applyHighlighting(searchSourceBuilder);

        return searchSourceBuilder;
    }

    private void applyHighlighting(SearchSourceBuilder searchSourceBuilder) {
        if (allowHighlighting) {
            final HighlightBuilder highlightBuilder = new HighlightBuilder()
                    .requireFieldMatch(false)
                    .field("*")
                    .fragmentSize(0)
                    .numOfFragments(0);
            searchSourceBuilder.highlighter(highlightBuilder);
        }
    }

    private void applyPaginationIfPresent(SearchSourceBuilder searchSourceBuilder, SearchCommand command) {
        command.offset().ifPresent(searchSourceBuilder::from);
        command.limit().ifPresent(searchSourceBuilder::size);
    }


    private void applyStreamsFilter(BoolQueryBuilder filteredQueryBuilder, SearchCommand command) {
        command.streams()
                .map(this::buildStreamIdFilter)
                .ifPresent(filteredQueryBuilder::filter);
    }

    private BoolQueryBuilder buildStreamIdFilter(Set<String> streams) {
        final BoolQueryBuilder filterBuilder = boolQuery();

        // If the included streams set contains the default stream, we also want all documents which do not
        // have any stream assigned. Those documents have basically been in the "default stream" which didn't
        // exist in Graylog <2.2.0.
        if (streams.contains(Stream.DEFAULT_STREAM_ID)) {
            filterBuilder.should(boolQuery().mustNot(existsQuery(Message.FIELD_STREAMS)));
        }

        // Only select messages which are assigned to the given streams
        filterBuilder.should(termsQuery(Message.FIELD_STREAMS, streams));

        return filterBuilder;
    }

    private void applySortingIfPresent(SearchSourceBuilder searchSourceBuilder, SearchCommand command) {
        final Sorting sort = command.sorting().orElse(DEFAULT_SORTING);
        searchSourceBuilder.sort(sort.getField(), sortOrderMapper.fromSorting(sort));
    }


    private boolean isWildcardQuery(String filter) {
        return normalizeQuery(filter).equals("*");
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "*";
        }
        return query.trim();
    }

}
