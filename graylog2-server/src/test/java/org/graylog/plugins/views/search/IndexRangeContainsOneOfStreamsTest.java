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
package org.graylog.plugins.views.search;

import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.streams.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexRangeContainsOneOfStreamsTest {
    private static final String indexName = "somethingsomething";

    @Mock
    private IndexRange indexRange;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Stream stream1;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Stream stream2;

    private IndexRangeContainsOneOfStreams toTest;

    @BeforeEach
    void setUp() {
        toTest = new IndexRangeContainsOneOfStreams();
    }

    @Test
    public void emptyStreamsShouldNotMatchAnything() {
        final IndexRange indexRange = mock(IndexRange.class);

        assertThat(toTest.test(indexRange, Collections.emptySet())).isFalse();
    }

    @Test
    public void currentIndexRangeShouldMatchIfManaged() {
        when(indexRange.streamIds()).thenReturn(null);
        when(indexRange.indexName()).thenReturn(indexName);

        when(stream1.getIndexSet().isManagedIndex(eq(indexName))).thenReturn(true);

        assertThat(toTest.test(indexRange, Set.of(stream1, stream2))).isTrue();
    }

    @Test
    public void currentIndexRangeShouldNotMatchIfNotManaged() {
        when(indexRange.streamIds()).thenReturn(null);
        when(indexRange.indexName()).thenReturn(indexName);

        when(stream1.getIndexSet().isManagedIndex(eq(indexName))).thenReturn(false);
        when(stream2.getIndexSet().isManagedIndex(eq(indexName))).thenReturn(false);

        assertThat(toTest.test(indexRange, Set.of(stream1, stream2))).isFalse();

        final ArgumentCaptor<String> stream1IndexSet = ArgumentCaptor.forClass(String.class);
        verify(stream1.getIndexSet(), times(1)).isManagedIndex(stream1IndexSet.capture());
        assertThat(stream1IndexSet.getAllValues()).containsExactly(indexName);

        final ArgumentCaptor<String> stream2IndexSet = ArgumentCaptor.forClass(String.class);
        verify(stream2.getIndexSet(), times(1)).isManagedIndex(stream2IndexSet.capture());
        assertThat(stream2IndexSet.getAllValues()).containsExactly(indexName);
    }

    @Test
    public void closedIndexRangeShouldNotMatchIfNotContainingStreamId() {
        when(indexRange.streamIds()).thenReturn(Collections.singletonList("stream3"));
        when(stream1.getId()).thenReturn("stream1");
        when(stream2.getId()).thenReturn("stream2");

        assertThat(toTest.test(indexRange, Set.of(stream1, stream2))).isFalse();

        verify(stream1.getIndexSet(), never()).isManagedIndex(any());
        verify(stream2.getIndexSet(), never()).isManagedIndex(any());
        verify(indexRange, never()).indexName();
    }

    @Test
    public void closedIndexRangeShouldNotMatchIfNotContainingAnyStreamId() {
        when(indexRange.streamIds()).thenReturn(Collections.emptyList());
        when(stream1.getId()).thenReturn("stream1");
        when(stream2.getId()).thenReturn("stream2");

        assertThat(toTest.test(indexRange, Set.of(stream1, stream2))).isFalse();

        verify(stream1.getIndexSet(), never()).isManagedIndex(any());
        verify(stream2.getIndexSet(), never()).isManagedIndex(any());
        verify(indexRange, never()).indexName();
    }
}
