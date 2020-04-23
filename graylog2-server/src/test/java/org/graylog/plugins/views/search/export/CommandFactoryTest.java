package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.TestData.validQueryBuilder;
import static org.graylog.plugins.views.search.export.TestData.validQueryBuilderWith;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandFactoryTest {

    private CommandFactory sut;
    private QueryStringDecorator queryStringDecorator;

    @BeforeEach
    void setUp() {
        queryStringDecorator = mock(QueryStringDecorator.class);
        when(queryStringDecorator.decorateQueryString(any(), any(), any())).then(returnsFirstArg());
        sut = new CommandFactory(queryStringDecorator);
    }

    @Test
    void takesRequestParamsFromSearch() {
        Query query = validQueryBuilder()
                .filter(streamFilter("stream-1", "stream-2"))
                .query(ElasticsearchQueryString.builder().queryString("huhu").build())
                .build();
        Search s = searchWithQueries(query);

        MessagesRequest request = buildFrom(s, query);

        assertThat(request.timeRange()).isEqualTo(query.timerange());
        assertThat(request.queryString()).isEqualTo(query.query());
        assertThat(request.streams()).isEqualTo(query.usedStreamIds());
    }

    @Test
    void takesRequestParamsFromResultFormat() {
        Query query = validQueryBuilder().build();
        Search s = searchWithQueries(query);

        ResultFormat resultFormat = ResultFormat.builder()
                .fieldsInOrder("field-1", "field-2")
                .sort(Sort.create("field-1", SortOrder.ASC))
                .limit(100)
                .build();

        MessagesRequest request = buildFrom(s, query, resultFormat);

        assertThat(request.sort()).isEqualTo(resultFormat.sort());
        assertThat(request.fieldsInOrder()).isEqualTo(resultFormat.fieldsInOrder());
        assertThat(request.limit()).isEqualTo(resultFormat.limit());
    }

    @Test
    void takesDefaultsIfNoResultsFormatSpecified() {
        Query query = validQueryBuilder().build();
        Search s = searchWithQueries(query);

        ResultFormat resultFormat = ResultFormat.builder()
                .build();

        MessagesRequest request = buildFrom(s, query, resultFormat);

        assertThat(request.sort()).isEqualTo(MessagesRequest.DEFAULT_SORT);
        assertThat(request.fieldsInOrder()).isEqualTo(MessagesRequest.DEFAULT_FIELDS);
    }

    @Test
    void takesStreamsFromSearchTypeIfNotEmpty() {
        MessageList ml = MessageList.builder().id("ml-id")
                .streams(ImmutableSet.of("stream-1", "stream-2"))
                .build();
        Query q = validQueryBuilderWith(ml).filter(streamFilter("stream-3")).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml);

        assertThat(request.streams()).isEqualTo(ml.effectiveStreams());
    }

    @Test
    void takesStreamsFromQueryIfEmptyOnSearchType() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).filter(streamFilter("stream-3")).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml);

        assertThat(request.streams()).isEqualTo(q.usedStreamIds());
    }

    @Test
    void takesQueryStringFromMessageListIfOnlySpecifiedThere() {
        MessageList ml = MessageList.builder().id("ml-id")
                .query(ElasticsearchQueryString.builder().queryString("nacken").build())
                .build();
        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml);

        //noinspection OptionalGetWithoutIsPresent
        assertThat(request.queryString()).isEqualTo(ml.query().get());
    }

    @Test
    void takesQueryStringFromQueryIfOnlySpecifiedThere() {
        MessageList ml = MessageList.builder().id("ml-id")
                .build();
        Query q = validQueryBuilderWith(ml)
                .query(ElasticsearchQueryString.builder().queryString("nacken").build())
                .build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml);

        assertThat(request.queryString()).isEqualTo(q.query());
    }

    @Test
    void combinesQueryStringIfSpecifiedOnMessageListAndQuery() {
        MessageList ml = MessageList.builder().id("ml-id")
                .query(ElasticsearchQueryString.builder().queryString("from-messagelist").build())
                .build();
        Query q = validQueryBuilderWith(ml)
                .query(ElasticsearchQueryString.builder().queryString("from-query").build())
                .build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml);

        ElasticsearchQueryString combined = ElasticsearchQueryString.builder()
                .queryString("from-query AND from-messagelist").build();

        assertThat(request.queryString())
                .isEqualTo(combined);
    }

    @Test
    void takesTimeRangeFromMessageListIfSpecified() {
        MessageList ml = MessageList.builder().id("ml-id").timerange(timeRange(111)).build();

        Query q = validQueryBuilderWith(ml).timerange(timeRange(222)).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml);

        assertThat(request.timeRange()).isEqualTo(timeRange(111));
    }

    @Test
    void takesTimeRangeFromQueryIfNotSpecifiedOnMessageList() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).timerange(timeRange(222)).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml);

        assertThat(request.timeRange()).isEqualTo(timeRange(222));
    }

    @Test
    void takesFieldsFromResultFormatIfSpecified() {
        MessageList ml = MessageList.builder().id("ml-id").build();
        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ResultFormat resultFormat = ResultFormat.builder().fieldsInOrder("field-1", "field-2").build();

        MessagesRequest request = buildFrom(s, q, ml, resultFormat);

        assertThat(request.fieldsInOrder()).isEqualTo(resultFormat.fieldsInOrder());
    }

    @Test
    void takesDefaultFieldsIfNotSpecifiedInResultFormat() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = buildFrom(s, q, ml, ResultFormat.builder().build());

        assertThat(request.fieldsInOrder()).isEqualTo(MessagesRequest.DEFAULT_FIELDS);
    }

    @Test
    void takesSortFromMessageListIfNotSpecifiedInResultFormat() {
        MessageList ml = MessageList.builder().id("ml-id")
                .sort(newArrayList(
                        Sort.create("field-1", SortOrder.ASC),
                        Sort.create("field-2", SortOrder.DESC)))
                .build();

        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ResultFormat resultFormat = ResultFormat.builder().build();

        MessagesRequest request = buildFrom(s, q, ml, resultFormat);

        assertThat(request.sort()).isEqualTo(new LinkedHashSet<>(requireNonNull(ml.sort())));
    }

    @Test
    void takesSortFromResultFormatIfSpecified() {
        MessageList ml = MessageList.builder().id("ml-id")
                .sort(newArrayList(
                        Sort.create("field-1", SortOrder.ASC),
                        Sort.create("field-2", SortOrder.DESC)))
                .build();

        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ResultFormat resultFormat = ResultFormat.builder().sort(Sort.create("field-3", SortOrder.ASC)).build();

        MessagesRequest request = buildFrom(s, q, ml, resultFormat);

        assertThat(request.sort()).isEqualTo(resultFormat.sort());
    }

    @Test
    void appliesQueryDecorators() {
        Query q = validQueryBuilder().query(ElasticsearchQueryString.builder().queryString("undecorated").build()).build();
        Search s = searchWithQueries(q);

        when(queryStringDecorator.decorateQueryString("undecorated", s, q)).thenReturn("decorated");

        MessagesRequest request = buildFrom(s, q);

        assertThat(request.queryString()).isEqualTo(ElasticsearchQueryString.builder().queryString("decorated").build());
    }

    private MessagesRequest buildFrom(Search s, Query query) {
        return buildFrom(s, query, ResultFormat.builder().build());
    }

    private MessagesRequest buildFrom(Search s, Query query, ResultFormat resultFormat) {
        return sut.buildWithSearchOnly(s, query, resultFormat);
    }

    private MessagesRequest buildFrom(Search search, Query query, MessageList messageList) {
        return buildFrom(search, query, messageList, ResultFormat.builder().build());
    }

    private MessagesRequest buildFrom(Search search, Query query, MessageList messageList, ResultFormat resultFormat) {
        return sut.buildWithMessageList(search, query, messageList, resultFormat);
    }

    private AndFilter streamFilter(String... streamIds) {
        StreamFilter[] filters = Arrays.stream(streamIds)
                .map(StreamFilter::ofId)
                .toArray(StreamFilter[]::new);
        return AndFilter.and(filters);
    }

    private Search searchWithQueries(Query... queries) {
        return Search.builder().id("search-id")
                .queries(ImmutableSet.copyOf(queries)).build();
    }

    private TimeRange timeRange(int range) {
        try {
            return RelativeRange.create(range);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }

}
