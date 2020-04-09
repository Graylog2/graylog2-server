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
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.graylog.plugins.views.search.TestData.validQueryBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class MessagesExporterTest {

    private ExportBackend backend;
    private MessagesExporter sut;

    @BeforeEach
    void setUp() {
        backend = mock(ExportBackend.class);
        sut = new MessagesExporter(new Defaults(), backend);
    }

    @Test
    void takesRequestParamsFromSearch() {
        Query query = validQueryBuilder()
                .filter(streamFilter("stream-1", "stream-2"))
                .query(ElasticsearchQueryString.builder().queryString("huhu").build())
                .build();
        Search s = searchWithQueries(query);

        MessagesRequest request = captureRequest(s);

        assertThat(request.timeRange()).contains(query.timerange());
        assertThat(request.queryString()).contains(query.query());
        assertThat(request.streams()).contains(query.usedStreamIds());
    }

    @Test
    void takesRequestParamsFromResultFormat() {
        Search s = searchWithQueries(validQueryBuilder().build());

        ResultFormat resultFormat = ResultFormat.builder()
                .fieldsInOrder("field-1", "field-2")
                .sort(Sort.create("field-1", SortOrder.ASC))
                .build();

        MessagesRequest request = captureRequest(s, resultFormat);

        assertThat(request.sort()).isEqualTo(resultFormat.sort());
        assertThat(request.fieldsInOrder()).isEqualTo(resultFormat.fieldsInOrder());
    }

    @Test
    void takesDefaultsIfNoResultsFormatSpecified() {
        Search s = searchWithQueries(validQueryBuilder().build());

        ResultFormat resultFormat = ResultFormat.builder()
                .build();

        MessagesRequest request = captureRequest(s, resultFormat);

        assertThat(request.sort()).contains(Defaults.DEFAULT_SORT);
        assertThat(request.fieldsInOrder()).contains(Defaults.DEFAULT_FIELDS);
    }

    @Test
    void takesStreamsFromSearchTypeIfNotEmpty() {
        MessageList ml = MessageList.builder().id("ml-id")
                .streams(ImmutableSet.of("stream-1", "stream-2"))
                .build();
        Query q = validQueryBuilderWith(ml).filter(streamFilter("stream-3")).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = captureRequest(s, ml.id(), ResultFormat.builder().build());

        assertThat(request.streams()).contains(ml.effectiveStreams());
    }

    @Test
    void takesStreamsFromQueryIfEmptyOnSearchType() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).filter(streamFilter("stream-3")).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = captureRequest(s, ml.id(), ResultFormat.builder().build());

        assertThat(request.streams()).contains(q.usedStreamIds());
    }

    @Test
    void takesQueryStringFromMessageListIfOnlySpecifiedThere() {
        MessageList ml = MessageList.builder().id("ml-id")
                .query(ElasticsearchQueryString.builder().queryString("nacken").build())
                .build();
        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = captureRequest(s, ml.id(), ResultFormat.builder().build());

        assertThat(request.queryString()).isEqualTo(ml.query());
    }

    @Test
    void takesQueryStringFromQueryIfOnlySpecifiedThere() {
        MessageList ml = MessageList.builder().id("ml-id")
                .build();
        Query q = validQueryBuilderWith(ml)
                .query(ElasticsearchQueryString.builder().queryString("nacken").build())
                .build();

        Search s = searchWithQueries(q);

        MessagesRequest request = captureRequest(s, ml.id(), ResultFormat.builder().build());

        assertThat(request.queryString()).contains(q.query());
    }

    @Test
    void addsAdditionalQueryStringIfSpecifiedOnMessageListAndQuery() {
        MessageList ml = MessageList.builder().id("ml-id")
                .query(ElasticsearchQueryString.builder().queryString("from-messagelist").build())
                .build();
        Query q = validQueryBuilderWith(ml)
                .query(ElasticsearchQueryString.builder().queryString("from-query").build())
                .build();

        Search s = searchWithQueries(q);

        MessagesRequest request = captureRequest(s, ml.id(), ResultFormat.builder().build());

        assertThat(request.queryString()).contains(q.query());
        assertThat(request.additionalQueryString()).isEqualTo(ml.query());
    }

    @Test
    void takesTimeRangeFromMessageListIfSpecified() {
        MessageList ml = MessageList.builder().id("ml-id").timerange(timeRange(111)).build();

        Query q = validQueryBuilderWith(ml).timerange(timeRange(222)).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = captureRequest(s, ml.id(), ResultFormat.builder().build());

        assertThat(request.timeRange()).contains(timeRange(111));
    }

    @Test
    void takesTimeRangeFromQueryIfNotSpecifiedOnMessageList() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).timerange(timeRange(222)).build();

        Search s = searchWithQueries(q);

        MessagesRequest request = captureRequest(s, ml.id(), ResultFormat.builder().build());

        assertThat(request.timeRange()).contains(timeRange(222));
    }

    @Test
    void takesFieldsFromResultFormatIfSpecified() {
        MessageList ml = MessageList.builder().id("ml-id").build();
        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ResultFormat resultFormat = ResultFormat.builder().fieldsInOrder("field-1", "field-2").build();

        MessagesRequest request = captureRequest(s, ml.id(), resultFormat);

        assertThat(request.fieldsInOrder()).isEqualTo(resultFormat.fieldsInOrder());
    }

    @Test
    void takesDefaultFieldsIfNotSpecifiedInResultFormat() {
        MessageList ml = MessageList.builder().id("ml-id").build();

        Query q = validQueryBuilderWith(ml).build();

        Search s = searchWithQueries(q);

        ResultFormat resultFormat = ResultFormat.builder().build();

        MessagesRequest request = captureRequest(s, ml.id(), resultFormat);

        assertThat(request.fieldsInOrder()).contains(Defaults.DEFAULT_FIELDS);
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

        MessagesRequest request = captureRequest(s, ml.id(), resultFormat);

        assertThat(request.sort()).contains(new LinkedHashSet<>(requireNonNull(ml.sort())));
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

        MessagesRequest request = captureRequest(s, ml.id(), resultFormat);

        assertThat(request.sort()).isEqualTo(resultFormat.sort());
    }

    @Test
    void throwsIfSearchTypeIsNotMessageList() {
        Pivot p = Pivot.builder().id("pivot-id").series(newArrayList()).rollup(false).build();

        Query q = validQueryBuilderWith(p).build();

        Search s = searchWithQueries(q);

        assertThatExceptionOfType(ExportException.class)
                .isThrownBy(() -> exportSearchType(s, p.id(), ResultFormat.builder().build()))
                .withMessageContaining("supported");
    }

    private Query.Builder validQueryBuilderWith(SearchType searchType) {
        return validQueryBuilder().searchTypes(ImmutableSet.of(searchType));
    }

    @Test
    void searchWithMultipleQueriesLeadsToException() {
        Search s = searchWithQueries(validQueryBuilder().build(), validQueryBuilder().build());

        assertThatExceptionOfType(ExportException.class)
                .isThrownBy(() -> exportSearch(s, ResultFormat.builder().build()))
                .withMessageContaining("multiple queries");
    }

    private AndFilter streamFilter(String... streamIds) {
        StreamFilter[] filters = Arrays.stream(streamIds)
                .map(StreamFilter::ofId)
                .toArray(StreamFilter[]::new);
        return AndFilter.and(filters);
    }

    private MessagesRequest captureRequest(Search search) {
        return captureRequest(search, ResultFormat.builder().build());
    }

    private MessagesRequest captureRequest(Search search, ResultFormat resultFormat) {
        return capture(() -> exportSearch(search, resultFormat));
    }

    private MessagesRequest captureRequest(Search search, String searchTypeId, ResultFormat resultFormat) {
        return capture(() -> exportSearchType(search, searchTypeId, resultFormat));
    }

    private MessagesRequest capture(Runnable call) {
        ArgumentCaptor<MessagesRequest> captor = ArgumentCaptor.forClass(MessagesRequest.class);

        doNothing().when(backend).run(captor.capture(), any());

        call.run();

        return captor.getValue();
    }

    private void exportSearchType(Search search, String searchTypeId, ResultFormat resultFormat) {
        sut.export(search, searchTypeId, resultFormat, x -> {
        });
    }

    private void exportSearch(Search search, ResultFormat resultFormat) {
        sut.export(search, resultFormat, x -> {
        });
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
