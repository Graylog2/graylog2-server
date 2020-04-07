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

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.SimpleMessage;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.Defaults.createDefaultMessagesRequest;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ElasticsearchExportBackendIT extends ElasticsearchBaseTest {

    private IndexLookup indexLookup;
    private ElasticsearchExportBackend sut;

    @Before
    public void setUp() {
        indexLookup = mock(IndexLookup.class);
        sut = new ElasticsearchExportBackend(jestClient(), indexLookup);
    }

    @Test
    public void usesCorrectIndicesAndStreams() {
        importFixture("messages.json");

        MessagesRequest request = requestBuilderWithAllStreams()
                .streams(ImmutableSet.of("stream-01", "stream-02"))
                .build();

        mockIndexLookupFor(request, "graylog_0", "graylog_1");

        runWithExpectedResult(request, "timestamp,source,message",
                "2015-01-01 04:00:00.000, source-2, Ho",
                "2015-01-01 02:00:00.000, source-2, He",
                "2015-01-01 01:00:00.000, source-1, Ha"
        );
    }

    private MessagesRequest.Builder requestBuilderWithAllStreams() {
        return defaultRequestBuilder().streams(ImmutableSet.of("stream-01", "stream-02", "stream-03"));
    }

    @Test
    public void usesQueryString() {
        importFixture("messages.json");

        MessagesRequest request = requestBuilderWithAllStreams()
                .queryString(ElasticsearchQueryString.builder().queryString("Ha Ho").build())
                .build();

        runWithExpectedResult(request, "timestamp,source,message",
                "2015-01-01 04:00:00.000, source-2, Ho",
                "2015-01-01 01:00:00.000, source-1, Ha"
        );
    }

    @Test
    public void usesAdditionalQueryStringIfPresent() {
        importFixture("messages.json");

        MessagesRequest request = requestBuilderWithAllStreams()
                .queryString(ElasticsearchQueryString.builder().queryString("H*").build())
                .additionalQueryString(ElasticsearchQueryString.builder().queryString("*a").build())
                .build();

        runWithExpectedResult(request, "timestamp,source,message",
                "2015-01-01 01:00:00.000, source-1, Ha"
        );
    }

    @Test
    public void usesTimeRange() {
        importFixture("messages.json");

        MessagesRequest request = requestBuilderWithAllStreams()
                .timeRange(timerange("2015-01-01T00:00:00.000Z", "2015-01-01T02:00:00.000Z"))
                .build();

        runWithExpectedResult(request, "timestamp,source,message",
                "2015-01-01 02:00:00.000, source-2, He",
                "2015-01-01 01:00:00.000, source-1, Ha"
        );
    }

    @Test
    public void usesFieldsInOrder() {
        importFixture("messages.json");

        MessagesRequest request = requestBuilderWithAllStreams()
                .fieldsInOrder("timestamp", "message")
                .build();

        runWithExpectedResult(request, "timestamp,message",
                "2015-01-01 04:00:00.000, Ho",
                "2015-01-01 03:00:00.000, Hi",
                "2015-01-01 02:00:00.000, He",
                "2015-01-01 01:00:00.000, Ha");
    }

    @Test
    public void usesSorting() {
        importFixture("messages.json");

        MessagesRequest request = requestBuilderWithAllStreams()
                .sort(linkedHashSetOf(Sort.create("source", SortOrder.ASC), Sort.create("timestamp", SortOrder.DESC)))
                .build();

        runWithExpectedResult(request, "timestamp,source,message",
                "2015-01-01 03:00:00.000, source-1, Hi",
                "2015-01-01 01:00:00.000, source-1, Ha",
                "2015-01-01 04:00:00.000, source-2, Ho",
                "2015-01-01 02:00:00.000, source-2, He");
    }

    @Test
    public void notifiesDoneOnNoMoreResults() {
        importFixture("messages.json");

        MessagesRequest request = requestBuilderWithAllStreams().build();

        List<String> invocations = new ArrayList<>();

        sut.run(request, h -> invocations.add(String.valueOf(h.size())), () -> invocations.add("done"));

        assertThat(invocations).containsExactly("4", "done");
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void mockIndexLookupFor(MessagesRequest request, String... indexNames) {
        when(indexLookup.indexNamesForStreamsInTimeRange(request.streams().get(), request.timeRange().get()))
                .thenReturn(ImmutableSet.copyOf(indexNames));
    }

    private MessagesRequest.Builder defaultRequestBuilder() {
        return createDefaultMessagesRequest().toBuilder()
                .timeRange(allMessagesTimeRange());
    }

    private void runWithExpectedResult(MessagesRequest request, String resultFields, String... resultMessages) {
        LinkedHashSet<SimpleMessage> totalResult = collectResultAsStringSet(request);

        LinkedHashSet<SimpleMessage> expected = parseToSimpleMessages(resultFields, resultMessages);

        assertThat(totalResult).isEqualTo(expected);
    }

    private LinkedHashSet<SimpleMessage> parseToSimpleMessages(String resultFields, String... messageStrings) {
        return Arrays.stream(messageStrings).map(s -> parse(resultFields, s)).collect(toCollection(LinkedHashSet::new));
    }

    private SimpleMessage parse(String fieldNames, String messageString) {
        String[] names = fieldNames.split(",");
        String[] values = messageString.split(",");
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        int i = 0;
        for (String name : names) {
            fields.put(name, values[i++].trim());
        }
        return SimpleMessage.from(fields);
    }

    private LinkedHashSet<SimpleMessage> collectResultAsStringSet(MessagesRequest request) {
        LinkedHashSet<SimpleMessage> totalResult = new LinkedHashSet<>();

        sut.run(request, totalResult::addAll, () -> {
        });

        return totalResult;
    }

    private TimeRange allMessagesTimeRange() {
        return timerange("2015-01-01T00:00:00.000Z", "2015-01-03T00:00:00.000Z");
    }

    @SuppressWarnings("SameParameterValue")
    private TimeRange timerange(String from, String to) {
        try {
            return AbsoluteRange.create(from, to);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }
}
