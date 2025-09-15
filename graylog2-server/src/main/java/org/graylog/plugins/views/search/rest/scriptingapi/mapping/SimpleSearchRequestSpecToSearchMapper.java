package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class SimpleSearchRequestSpecToSearchMapper implements SearchRequestSpecToSearchMapper {
    private final AggregationSpecToPivotMapper pivotCreator;
    private final MessagesSpecToMessageListMapper messageListCreator;
    private final Function<Collection<String>, Stream<String>> streamCategoryMapper;

    @Inject
    public SimpleSearchRequestSpecToSearchMapper(final AggregationSpecToPivotMapper pivotCreator,
                                           final MessagesSpecToMessageListMapper messageListCreator,
                                           StreamService streamService) {
        this.pivotCreator = pivotCreator;
        this.messageListCreator = messageListCreator;
        this.streamCategoryMapper = streamService::mapCategoriesToIds;
    }

    public Search mapToSearch(MessagesRequestSpec messagesRequestSpec, SearchUser searchUser) {
        return mapToSearch(messagesRequestSpec, searchUser, messageListCreator);
    }

    public Search mapToSearch(AggregationRequestSpec aggregationRequestSpec, SearchUser searchUser) {
        return mapToSearch(aggregationRequestSpec, searchUser, pivotCreator);
    }

    protected String getQueryString(final SearchRequestSpec searchRequestSpec) {
        return searchRequestSpec.queryString();
    }

    private <T extends SearchRequestSpec> Search mapToSearch(final T searchRequestSpec, final SearchUser searchUser, Function<T, ? extends SearchType> searchTypeCreator) {
        Query query = Query.builder()
                .id(QUERY_ID)
                .searchTypes(Set.of(searchTypeCreator.apply(searchRequestSpec)))
                .query(ElasticsearchQueryString.ofNullable(getQueryString(searchRequestSpec)))
                .timerange(getTimerange(searchRequestSpec))
                .build();

        if (!(searchRequestSpec.streams().isEmpty() && searchRequestSpec.streamCategories().isEmpty())) {
            query = query.orStreamAndStreamCategoryFilters(
                    new HashSet<>(searchRequestSpec.streams()),
                    new HashSet<>(searchRequestSpec.streamCategories())
            );
        }

        return Search.builder()
                .queries(ImmutableSet.of(query))
                .build()
                .addStreamsToQueriesWithCategories(streamCategoryMapper, searchUser)
                .addStreamsToQueriesWithoutStreams(() -> searchUser.streams().readableOrAllIfEmpty(searchRequestSpec.streams()));
    }

    private TimeRange getTimerange(SearchRequestSpec searchRequestSpec) {
        return searchRequestSpec.timerange() != null ? searchRequestSpec.timerange() : RelativeRange.allTime();
    }
}
