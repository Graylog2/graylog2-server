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
package org.graylog.plugins.views.search.elasticsearch;

import io.searchbox.core.MultiSearch;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendSearchTypesWithStreamsOverridesTest extends ElasticsearchBackendGeneratedRequestTestBase {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private final String stream1Id = "stream1Id";
    private final String stream2Id = "stream2Id";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Stream stream1;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Stream stream2;

    @Before
    public void setUp() throws Exception {
        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("successfulMultiSearchResponse.json")));
        final IndexRange indexRange1 = mock(IndexRange.class);
        when(indexRange1.indexName()).thenReturn("index1");
        when(indexRange1.streamIds()).thenReturn(Collections.singletonList(stream1Id));
        final IndexRange indexRange2 = mock(IndexRange.class);
        when(indexRange2.indexName()).thenReturn("index2");
        when(indexRange2.streamIds()).thenReturn(Collections.singletonList(stream1Id));
        final IndexRange indexRange3 = mock(IndexRange.class);
        when(indexRange3.indexName()).thenReturn("index3");
        when(indexRange3.streamIds()).thenReturn(Collections.singletonList(stream2Id));

        final SortedSet<IndexRange> indexRanges = sortedSetOf(indexRange1, indexRange2, indexRange3);
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indexRanges);

        when(stream1.getId()).thenReturn(stream1Id);
        when(stream2.getId()).thenReturn(stream2Id);
        when(streamService.loadByIds(Collections.singleton(stream1Id))).thenReturn(Collections.singleton(stream1));
        when(streamService.loadByIds(Collections.singleton(stream2Id))).thenReturn(Collections.singleton(stream2));
    }

    @Test
    public void searchTypeWithEmptyStreamsDefaultsToQueriesStreams() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .streams(Collections.emptySet())
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index1,index2");
    }

    @Test
    public void searchTypeWithoutStreamsDefaultsToQueriesStreams() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index1,index2");
    }

    @Test
    public void searchTypeWithStreamsOverridesQueriesStreams() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .streams(Collections.singleton(stream2Id))
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index3");
    }

    @Test
    public void queryWithMixedPresenceOfOverridesIncludesMultipleSetsOfIndices() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .streams(Collections.singleton(stream2Id))
                                .build(),
                        Pivot.builder()
                                .id("pivot2")
                                .series(Collections.singletonList(Max.builder().field("field2").build()))
                                .rollup(true)
                                .streams(Collections.emptySet())
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index3");
        assertThat(indicesOf(request).get(1)).isEqualTo("index1,index2");
    }

    private Query queryFor(SearchType... searchTypes) {
        return Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .timerange(timeRangeForTest())
                .filter(StreamFilter.ofId(stream1Id))
                .searchTypes(Arrays.stream(searchTypes).collect(Collectors.toSet()))
                .build();
    }

    private MultiSearch run(Query query) throws IOException {
        final SearchJob job = searchJobForQuery(query);
        final ESGeneratedQueryContext context = this.elasticsearchBackend.generate(job, query, Collections.emptySet());

        this.elasticsearchBackend.doRun(job, query, context, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        return clientRequestCaptor.getValue();
    }
}
