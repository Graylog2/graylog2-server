/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.views.export;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.plugins.views.search.export.SimpleMessage;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.export.TestData;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public abstract class ExportITHelper {

    protected final IndexLookup indexLookup;

    public ExportITHelper(final IndexLookup indexLookup) {
        this.indexLookup = indexLookup;
    }

    public abstract LinkedHashSet<SimpleMessageChunk> collectChunksFor(final ExportMessagesCommand command);

    public ExportMessagesCommand.Builder commandBuilderWithAllTestDefaultStreams() {
        return ExportMessagesCommand.withDefaults()
                .toBuilder()
                .timeRange(allMessagesInTestsDefaultTimeRange())
                .streams(ImmutableSet.of("stream-01", "stream-02", "stream-03"));
    }

    public Set<String> actualFieldNamesFrom(SimpleMessageChunk chunk) {
        return chunk.messages()
                .stream()
                .map(m -> m.fields().keySet())
                .reduce(Sets::union)
                .orElseThrow(() -> new RuntimeException("failed to collect field names"));
    }

    public AbsoluteRange allMessagesInTestsDefaultTimeRange() {
        return AbsoluteRange.create("2015-01-01T00:00:00.000Z", "2015-01-03T00:00:00.000Z");
    }

    public Object[] toObjectArray(String s) {
        return Arrays.stream(s.split(",")).map(String::trim).toArray();
    }

    public void mockIndexLookupFor(ExportMessagesCommand command, String... indexNames) {
        when(indexLookup.indexNamesForStreamsInTimeRange(command.streams(), command.timeRange()))
                .thenReturn(ImmutableSet.copyOf(indexNames));
    }

    public void keepOnlyRelevantFields(SimpleMessageChunk chunk, LinkedHashSet<String> relevantFields) {
        for (SimpleMessage msg : chunk.messages()) {
            Set<String> allFieldsInMessage = ImmutableSet.copyOf(msg.fields().keySet());
            for (String name : allFieldsInMessage) {
                if (!relevantFields.contains(name)) {
                    msg.fields().remove(name);
                }
            }
        }
    }

    public void runWithExpectedResult(ExportMessagesCommand command, @SuppressWarnings("SameParameterValue") String resultFields, String... messageValues) {
        SimpleMessageChunk totalResult = collectTotalResult(command);

        assertResultMatches(resultFields, totalResult, messageValues, true);
    }

    public void runWithExpectedResultIgnoringSort(ExportMessagesCommand command, String resultFields, String... messageValues) {
        SimpleMessageChunk totalResult = collectTotalResult(command);

        assertResultMatches(resultFields, totalResult, messageValues, false);
    }

    public void assertResultMatches(String resultFields, SimpleMessageChunk totalResult, String[] messageValues, boolean expectSorted) {
        Object[][] values = Arrays.stream(messageValues).map(this::toObjectArray).toArray(Object[][]::new);

        SimpleMessageChunk expected = TestData.simpleMessageChunkWithIndexNames(resultFields, values);

        assertThat(totalResult).isEqualTo(expected);

        if (expectSorted) {
            assertThat(totalResult.messages()).containsExactlyElementsOf(expected.messages());
        }
    }

    public SimpleMessageChunk collectTotalResult(ExportMessagesCommand command) {
        LinkedHashSet<SimpleMessageChunk> allChunks = collectChunksFor(command);

        LinkedHashSet<SimpleMessage> allMessages = new LinkedHashSet<>();

        for (SimpleMessageChunk chunk : allChunks) {
            keepOnlyRelevantFields(chunk, command.fieldsInOrder());
            allMessages.addAll(chunk.messages());
        }

        return SimpleMessageChunk.from(command.fieldsInOrder(), allMessages);
    }
}
