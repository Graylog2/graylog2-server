package org.graylog.storage.elasticsearch7;

import com.google.auto.value.AutoValue;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.graylog2.indexer.searches.ScrollCommand;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
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
        return create(SearchRequestFactory.Command.from(config));
    }

    public SearchSourceBuilder create(ScrollCommand scrollCommand) {
        return create(SearchRequestFactory.Command.from(scrollCommand));
    }

    public SearchSourceBuilder create(Command command) {
        final String query = normalizeQuery(command.query());

        final QueryBuilder queryBuilder = isWildcardQuery(query)
                ? matchAllQuery()
                : queryStringQuery(query).allowLeadingWildcard(allowLeadingWildcardSearches);

        final Optional<BoolQueryBuilder> rangeQueryBuilder = command.range()
                .map(range -> boolQuery()
                        .must(TimeRangeQueryFactory.create(range)));
        final Optional<BoolQueryBuilder> filterQueryBuilder = command.filter()
                .filter(filter -> !isWildcardQuery(filter))
                .map(QueryBuilders::queryStringQuery)
                .map(filter -> rangeQueryBuilder.orElse(boolQuery())
                        .must(filter));

        final BoolQueryBuilder filteredQueryBuilder = boolQuery()
                .must(queryBuilder);
        filterQueryBuilder.ifPresent(filteredQueryBuilder::filter);

        applyStreamsFilter(filteredQueryBuilder, command);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(filteredQueryBuilder);

        applyPaginationIfPresent(searchSourceBuilder, command);

        applySortingIfPresent(searchSourceBuilder, command);

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

    private void applyPaginationIfPresent(SearchSourceBuilder searchSourceBuilder, Command command) {
        command.offset().ifPresent(searchSourceBuilder::from);
        command.limit().ifPresent(searchSourceBuilder::size);
    }


    private void applyStreamsFilter(BoolQueryBuilder filteredQueryBuilder, Command command) {
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

    private void applySortingIfPresent(SearchSourceBuilder searchSourceBuilder, Command command) {
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

    @Nullable
    private QueryBuilder standardFilters(TimeRange range, String filter) {
        BoolQueryBuilder bfb = null;

        if (range != null) {
            bfb = QueryBuilders.boolQuery()
                    .must(TimeRangeQueryFactory.create(range));
        }

        // Not creating a filter for a "*" value because an empty filter used to be submitted that way.
        if (!isNullOrEmpty(filter) && !"*".equals(filter)) {
            if (bfb == null) {
                bfb = QueryBuilders.boolQuery();
            }
            bfb.must(queryStringQuery(filter));
        }

        return bfb;
    }

    @AutoValue
    static abstract class Command {
        public abstract String query();
        public abstract Optional<Set<String>> streams();
        public abstract Optional<Sorting> sorting();
        public abstract Optional<String> filter();
        public abstract Optional<TimeRange> range();
        public abstract OptionalInt limit();
        public abstract OptionalInt offset();
        public abstract OptionalLong batchSize();
        public abstract boolean highlight();

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static Command create(
                String query,
                Optional<Set<String>> streams,
                Optional<Sorting> sorting,
                Optional<String> filter,
                Optional<TimeRange> range,
                OptionalInt limit,
                OptionalInt offset,
                OptionalLong batchSize,
                boolean highlight) {
            return new AutoValue_SearchRequestFactory_Command(query, streams, sorting, filter, range, limit, offset, batchSize, highlight);
        }

        static Command from(SearchesConfig searchesConfig) {
            return create(searchesConfig.query(), Optional.empty(), Optional.ofNullable(searchesConfig.sorting()),
                    Optional.ofNullable(searchesConfig.filter()), Optional.of(searchesConfig.range()),
                    OptionalInt.of(searchesConfig.limit()), OptionalInt.of(searchesConfig.offset()),
                    OptionalLong.empty(), true);
        }

        static Command from(ScrollCommand scrollCommand) {
            return create(scrollCommand.query(), scrollCommand.streams(), scrollCommand.sorting(),
                    scrollCommand.filter(), scrollCommand.range(), scrollCommand.limit(), scrollCommand.offset(),
                    scrollCommand.batchSize(), scrollCommand.highlight());
        }
    }
}
