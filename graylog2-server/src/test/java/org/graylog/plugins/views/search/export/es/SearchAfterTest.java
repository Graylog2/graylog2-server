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
package org.graylog.plugins.views.search.export.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import net.minidev.json.JSONArray;
import org.elasticsearch.common.util.set.Sets;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.es.SearchAfter.DEFAULT_TIEBREAKER_FIELD;
import static org.graylog.plugins.views.search.export.es.SearchAfter.EVENTS_TIEBREAKER_FIELD;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENT_STREAM_IDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SearchAfterTest {
    private SearchAfter sut;
    private JestWrapper jestWrapper;
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @BeforeEach
    void setUp() {
        jestWrapper = mock(JestWrapper.class);
        SearchResult emptyResult = new SearchResult(new ObjectMapper());

        doReturn(emptyResult).when(jestWrapper).execute(any(), any());
        sut = new SearchAfter(jestWrapper);
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

        sut.nextChunk(new Search.Builder(""), command);

        List<String> sortKeys = captureSortKeys();

        assertThat(sortKeys).containsExactly(FIELD_TIMESTAMP, EVENTS_TIEBREAKER_FIELD);
    }

    @Test
    void usesDefaultTieBreakerForNonEventStreams() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults().toBuilder()
                .streams("stream-1", "stream-2").build();

        sut.nextChunk(new Search.Builder(""), command);

        List<String> sortKeys = captureSortKeys();

        assertThat(sortKeys).containsExactly(FIELD_TIMESTAMP, DEFAULT_TIEBREAKER_FIELD);
    }

    private List<String> captureSortKeys() {
        //noinspection unchecked
        ArgumentCaptor<Action<? extends JestResult>> captor = ArgumentCaptor.forClass(Action.class);

        verify(jestWrapper).execute(captor.capture(), any());

        return sortKeysFrom(captor.getValue());
    }

    private List<String> sortKeysFrom(Action<? extends JestResult> searchAction) {
        String rawJson = dataFrom(searchAction);
        JSONArray sorts = JsonPath.parse(rawJson).read("$.sort.*");
        return sorts.stream().map(this::singleKeyFrom).collect(Collectors.toList());
    }

    private String singleKeyFrom(Object jsonMap) {
        //noinspection unchecked
        return ((Map<String, Object>) jsonMap).keySet().iterator().next();
    }

    private String dataFrom(Action<? extends JestResult> searchAction) {
        try {
            return searchAction.getData(objectMapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
