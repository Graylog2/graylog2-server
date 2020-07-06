package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.ScrollCommand;
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

public class Scroll {
    private static final String DEFAULT_SCROLLTIME = "1m";
    private static final Sorting DEFAULT_SORTING = new Sorting("_doc", Sorting.Direction.ASC);
    private final ElasticsearchClient client;
    private final ScrollResultES7.Factory scrollResultFactory;
    private final SortOrderMapper sortOrderMapper;
    private final boolean allowLeadingWildcardSearches;
    private final boolean allowHighlighting;

    @Inject
    public Scroll(ElasticsearchClient client,
                  ScrollResultES7.Factory scrollResultFactory,
                  SortOrderMapper sortOrderMapper,
                  @Named("allow_leading_wildcard_searches") boolean allowLeadingWildcardSearches,
                  @Named("allow_highlighting") boolean allowHighlighting) {
        this.client = client;
        this.scrollResultFactory = scrollResultFactory;
        this.sortOrderMapper = sortOrderMapper;
        this.allowLeadingWildcardSearches = allowLeadingWildcardSearches;
        this.allowHighlighting = allowHighlighting;
    }

    public ScrollResult scroll(ScrollCommand scrollCommand) {
        final SearchSourceBuilder searchQuery = buildSearchRequest(scrollCommand);
        searchQuery.fetchSource(scrollCommand.fields().toArray(new String[0]), new String[0]);
        scrollCommand.batchSize()
                .ifPresent(batchSize -> searchQuery.size(Math.toIntExact(batchSize)));
        final SearchRequest request = scrollBuilder(searchQuery, scrollCommand.indices());

        final SearchResponse result = client.search(request, "Unable to perform scroll search");

        return scrollResultFactory.create(result, searchQuery.toString(), DEFAULT_SCROLLTIME, scrollCommand.fields());
    }

    private SearchRequest scrollBuilder(SearchSourceBuilder query, Set<String> indices) {
        return new SearchRequest(indices.toArray(new String[0]))
                .source(query)
                .scroll(DEFAULT_SCROLLTIME);
    }

    private SearchSourceBuilder buildSearchRequest(ScrollCommand scrollCommand) {
        final String query = normalizeQuery(scrollCommand.query());

        final QueryBuilder queryBuilder = isWildcardQuery(query)
                ? matchAllQuery()
                : queryStringQuery(query).allowLeadingWildcard(allowLeadingWildcardSearches);

        final Optional<BoolQueryBuilder> rangeQueryBuilder = scrollCommand.range()
                .map(range -> boolQuery()
                        .must(TimeRangeQueryFactory.create(range)));
        final Optional<BoolQueryBuilder> filterQueryBuilder = scrollCommand.filter()
                .filter(filter -> !isWildcardQuery(filter))
                .map(QueryBuilders::queryStringQuery)
                .map(filter -> rangeQueryBuilder.orElse(boolQuery())
                        .must(filter));

        final BoolQueryBuilder filteredQueryBuilder = boolQuery()
                .must(queryBuilder);
        filterQueryBuilder.ifPresent(filteredQueryBuilder::filter);

        applyStreamsFilter(filteredQueryBuilder, scrollCommand);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(filteredQueryBuilder);

        applyPaginationIfPresent(searchSourceBuilder, scrollCommand);

        applySortingIfPresent(searchSourceBuilder, scrollCommand);

        applyHighlighting(searchSourceBuilder, scrollCommand);

        return searchSourceBuilder;
    }

    private void applyStreamsFilter(BoolQueryBuilder filteredQueryBuilder, ScrollCommand scrollCommand) {
        scrollCommand.streams()
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

    private void applyPaginationIfPresent(SearchSourceBuilder searchSourceBuilder, ScrollCommand scrollCommand) {
        scrollCommand.offset().ifPresent(searchSourceBuilder::from);
        scrollCommand.limit().ifPresent(searchSourceBuilder::size);
    }

    private void applyHighlighting(SearchSourceBuilder searchSourceBuilder, ScrollCommand scrollCommand) {
        if (scrollCommand.highlight() && allowHighlighting) {
            final HighlightBuilder highlightBuilder = new HighlightBuilder()
                    .requireFieldMatch(false)
                    .field("*")
                    .fragmentSize(0)
                    .numOfFragments(0);
            searchSourceBuilder.highlighter(highlightBuilder);
        }
    }

    private void applySortingIfPresent(SearchSourceBuilder searchSourceBuilder, ScrollCommand scrollCommand) {
        final Sorting sort = scrollCommand.sorting().orElse(DEFAULT_SORTING);
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
