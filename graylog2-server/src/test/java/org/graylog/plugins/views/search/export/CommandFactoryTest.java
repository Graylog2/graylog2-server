/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import org.graylog2.decorators.Decorator;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
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
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_FIELDS;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_SORT;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.defaultTimeRange;
import static org.graylog.plugins.views.search.export.TestData.relativeRange;
import static org.graylog.plugins.views.search.export.TestData.validQueryBuilder;
import static org.graylog.plugins.views.search.export.TestData.validQueryBuilderWith;
import static org.junit.jupiter.api.Assertions.assertAll;
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
    void buildsCommandFromRequest() {
        MessagesRequest request = MessagesRequest.builder().build();
        ExportMessagesCommand command = sut.buildFromRequest(request);

        assertAll(
                () -> assertThat(command.queryString()).isEqualTo(request.queryString()),
                () -> assertThat(command.streams()).isEqualTo(request.streams()),
                () -> assertThat(command.fieldsInOrder()).isEqualTo(request.fieldsInOrder()),
                () -> assertThat(command.sort()).isEqualTo(request.sort()),
                () -> assertThat(command.limit()).isEqualTo(request.limit())
        );
    }

    @Test
    void convertsTimeRangeToAbsolute() {
        RelativeRange relative = relativeRange(100);
        MessagesRequest request = MessagesRequest.builder().timeRange(relative).build();

        ExportMessagesCommand command = sut.buildFromRequest(request);

        assertThat(command.timeRange()).isInstanceOf(AbsoluteRange.class);
    }

    @Test
    void takesRequestParamsFromSearch() {
        Query query = validQueryBuilder()
                .filter(streamFilter("stream-1", "stream-2"))
                .query(ElasticsearchQueryString.builder().queryString("huhu").build())
                .build();
        Search s = searchWithQueries(query);

        ExportMessagesCommand command = buildFrom(s, query);

        assertThat(command.timeRange()).isEqualTo(query.timerange());
        assertThat(command.queryString()).isEqualTo(query.query());
        assertThat(command.streams()).isEqualTo(query.usedStreamIds());
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

        ExportMessagesCommand command = buildFrom(s, query, resultFormat);

        assertThat(command.sort()).isEqualTo(resultFormat.sort());
        assertThat(command.fieldsInOrder()).isEqualTo(resultFormat.fieldsInOrder());
        assertThat(command.limit()).isEqualTo(resultFormat.limit());
    }

    @Test
    void takesDefaultsIfNoResultsFormatSpecified() {
        Query query = validQueryBuilder().build();
        Search s = searchWithQueries(query);

        ResultFormat resultFormat = ResultFormat.builder()
                .build();

        ExportMessagesCommand command = buildFrom(s, query, resultFormat);

        assertThat(command.sort()).isEqualTo(DEFAULT_SORT);
        assertThat(command.fieldsInOrder()).isEqualTo(DEFAULT_FIELDS);
    }

    @Test
    void takesStreamsFromSearchTypeIfNotEmpty() {
        MessageList ml = MessageList.builder().id("ml-id")
                .streams(ImmutableSet.of("stream-1", "stream-2"))
                .build();
        Query q = validQueryBuilderWith(ml).filter(streamFilter("stream-3")).build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml);

        assertThat(command.streams()).isEqualTo(ml.effectiveStreams());
    }

    @Test
    void takesStreamsFromQueryIfEmptyOnSearchType() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).filter(streamFilter("stream-3")).build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml);

        assertThat(command.streams()).isEqualTo(q.usedStreamIds());
    }

    @Test
    void takesQueryStringFromMessageListIfOnlySpecifiedThere() {
        MessageList ml = MessageList.builder().id("ml-id")
                .query(ElasticsearchQueryString.builder().queryString("nacken").build())
                .build();
        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml);

        //noinspection OptionalGetWithoutIsPresent
        assertThat(command.queryString()).isEqualTo(ml.query().get());
    }

    @Test
    void takesQueryStringFromQueryIfOnlySpecifiedThere() {
        MessageList ml = MessageList.builder().id("ml-id")
                .build();
        Query q = validQueryBuilderWith(ml)
                .query(ElasticsearchQueryString.builder().queryString("nacken").build())
                .build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml);

        assertThat(command.queryString()).isEqualTo(q.query());
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

        ExportMessagesCommand command = buildFrom(s, q, ml);

        ElasticsearchQueryString combined = ElasticsearchQueryString.builder()
                .queryString("from-query AND from-messagelist").build();

        assertThat(command.queryString())
                .isEqualTo(combined);
    }

    @Test
    void takesTimeRangeFromMessageListIfSpecified() {
        AbsoluteRange messageListTimeRange = defaultTimeRange();
        MessageList ml = MessageList.builder().id("ml-id").timerange(messageListTimeRange).build();

        Query q = validQueryBuilderWith(ml).timerange(timeRange(222)).build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml);

        assertThat(command.timeRange()).isEqualTo(messageListTimeRange);
    }

    @Test
    void takesTimeRangeFromQueryIfNotSpecifiedOnMessageList() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml);

        assertThat(command.timeRange()).isEqualTo(q.timerange());
    }

    @Test
    void takesDecoratorsFromMessageList() {
        Decorator decorator = mock(Decorator.class);
        MessageList ml = MessageList.builder().id("ml-id")
                .decorators(newArrayList(decorator))
                .build();
        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml);

        assertThat(command.decorators()).containsExactly(decorator);
    }

    @Test
    void takesFieldsFromResultFormatIfSpecified() {
        MessageList ml = MessageList.builder().id("ml-id").build();
        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ResultFormat resultFormat = ResultFormat.builder().fieldsInOrder("field-1", "field-2").build();

        ExportMessagesCommand command = buildFrom(s, q, ml, resultFormat);

        assertThat(command.fieldsInOrder()).isEqualTo(resultFormat.fieldsInOrder());
    }

    @Test
    void takesDefaultFieldsIfNotSpecifiedInResultFormat() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ExportMessagesCommand command = buildFrom(s, q, ml, ResultFormat.builder().build());

        assertThat(command.fieldsInOrder()).isEqualTo(DEFAULT_FIELDS);
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

        ExportMessagesCommand command = buildFrom(s, q, ml, resultFormat);

        assertThat(command.sort()).isEqualTo(new LinkedHashSet<>(requireNonNull(ml.sort())));
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

        ExportMessagesCommand command = buildFrom(s, q, ml, resultFormat);

        assertThat(command.sort()).isEqualTo(resultFormat.sort());
    }

    @Test
    void appliesQueryDecorators() {
        Query q = validQueryBuilder().query(ElasticsearchQueryString.builder().queryString("undecorated").build()).build();
        Search s = searchWithQueries(q);

        when(queryStringDecorator.decorateQueryString("undecorated", s, q)).thenReturn("decorated");

        ExportMessagesCommand command = buildFrom(s, q);

        assertThat(command.queryString()).isEqualTo(ElasticsearchQueryString.builder().queryString("decorated").build());
    }

    private ExportMessagesCommand buildFrom(Search s, Query query) {
        return buildFrom(s, query, ResultFormat.builder().build());
    }

    private ExportMessagesCommand buildFrom(Search s, Query query, ResultFormat resultFormat) {
        return sut.buildWithSearchOnly(s, query, resultFormat);
    }

    private ExportMessagesCommand buildFrom(Search search, Query query, MessageList messageList) {
        return buildFrom(search, query, messageList, ResultFormat.builder().build());
    }

    private ExportMessagesCommand buildFrom(Search search, Query query, MessageList messageList, ResultFormat resultFormat) {
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
