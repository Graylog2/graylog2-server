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
package org.graylog.storage.elasticsearch7.views.export;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.search.TotalHits;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHits;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.storage.elasticsearch7.views.export.SearchAfter.DEFAULT_TIEBREAKER_FIELD;
import static org.graylog.storage.elasticsearch7.views.export.SearchAfter.EVENTS_TIEBREAKER_FIELD;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENT_STREAM_IDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchAfterTest {
    private SearchAfter sut;
    private ExportClient client;

    @BeforeEach
    void setUp() {
        client = mock(ExportClient.class);
        final SearchResponse emptyResult = mock(SearchResponse.class);
        when(emptyResult.getHits()).thenReturn(new SearchHits(new SearchHit[0], new TotalHits(0, TotalHits.Relation.EQUAL_TO), 0.0f));

        doReturn(emptyResult).when(client).search(any(), any());
        sut = new SearchAfter(client);
    }

    @Test
    void ignoresEventsStreamsForAMixOfEventStreamsAndOthers() {
        ImmutableSet<String> eventStreams = ImmutableSet.copyOf(DEFAULT_EVENT_STREAM_IDS);
        ImmutableSet<String> allowedStream = ImmutableSet.of("allowed-stream");
        Set<String> requestedStreams = Sets.union(eventStreams, allowedStream);

        Set<String> supportedStreams = sut.removeUnsupportedStreams(requestedStreams);

        assertThat(supportedStreams).containsOnly("allowed-stream");
    }

    @Test
    void supportsEventsStreamsOnly() {
        ImmutableSet<String> eventStreams = ImmutableSet.copyOf(DEFAULT_EVENT_STREAM_IDS);

        Set<String> supportedStreams = sut.removeUnsupportedStreams(eventStreams);

        assertThat(supportedStreams).containsOnlyElementsOf(DEFAULT_EVENT_STREAM_IDS);
    }

    @Test
    void usesIDAsTieBreakerForEventStreams() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults().toBuilder()
                .streams(DEFAULT_EVENT_STREAM_IDS.asList().get(0)).build();

        sut.nextChunk(new SearchRequest().source(new SearchSourceBuilder()), command);

        List<String> sortKeys = captureSortKeys();

        assertThat(sortKeys).containsExactly(FIELD_TIMESTAMP, EVENTS_TIEBREAKER_FIELD);
    }

    @Test
    void usesDefaultTieBreakerForNonEventStreams() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults().toBuilder()
                .streams("stream-1", "stream-2").build();

        sut.nextChunk(new SearchRequest().source(new SearchSourceBuilder()), command);

        List<String> sortKeys = captureSortKeys();

        assertThat(sortKeys).containsExactly(FIELD_TIMESTAMP, DEFAULT_TIEBREAKER_FIELD);
    }

    private List<String> captureSortKeys() {
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        verify(client).search(captor.capture(), any());

        return sortKeysFrom(captor.getValue().source());
    }

    private List<String> sortKeysFrom(SearchSourceBuilder searchAction) {
        String rawJson = dataFrom(searchAction);
        JSONArray sorts = JsonPath.parse(rawJson).read("$.sort.*");
        return sorts.stream().map(this::singleKeyFrom).collect(Collectors.toList());
    }

    private String singleKeyFrom(Object jsonMap) {
        //noinspection unchecked
        return ((Map<String, Object>) jsonMap).keySet().iterator().next();
    }

    private String dataFrom(SearchSourceBuilder searchAction) {
        return searchAction.toString();
    }
}
