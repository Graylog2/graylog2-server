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
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexRangeContainsOneOfStreamsTest {
    private static final String indexName = "somethingsomething";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private IndexRange indexRange;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Stream stream1;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Stream stream2;

    @Test
    public void emptyStreamsShouldNotMatchAnything() {
        final IndexRangeContainsOneOfStreams predicate = new IndexRangeContainsOneOfStreams(Collections.emptySet());
        final IndexRange indexRange = mock(IndexRange.class);

        assertThat(predicate.test(indexRange)).isFalse();
    }

    @Test
    public void currentIndexRangeShouldMatchIfManaged() {
        when(indexRange.streamIds()).thenReturn(null);
        when(indexRange.indexName()).thenReturn(indexName);

        when(stream1.getId()).thenReturn("stream1");
        when(stream1.getIndexSet().isManagedIndex(eq(indexName))).thenReturn(true);

        when(stream2.getId()).thenReturn("stream2");
        when(stream2.getIndexSet().isManagedIndex(eq(indexName))).thenReturn(false);

        final IndexRangeContainsOneOfStreams predicate = new IndexRangeContainsOneOfStreams(stream1, stream2);

        assertThat(predicate.test(indexRange)).isTrue();
    }

    @Test
    public void currentIndexRangeShouldNotMatchIfNotManaged() {
        when(indexRange.streamIds()).thenReturn(null);
        when(indexRange.indexName()).thenReturn(indexName);

        when(stream1.getId()).thenReturn("stream1");
        when(stream1.getIndexSet().isManagedIndex(eq(indexName))).thenReturn(false);

        when(stream2.getId()).thenReturn("stream2");
        when(stream2.getIndexSet().isManagedIndex(eq(indexName))).thenReturn(false);

        final IndexRangeContainsOneOfStreams predicate = new IndexRangeContainsOneOfStreams(stream1, stream2);

        assertThat(predicate.test(indexRange)).isFalse();

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

        final IndexRangeContainsOneOfStreams predicate = new IndexRangeContainsOneOfStreams(stream1, stream2);

        assertThat(predicate.test(indexRange)).isFalse();

        verify(stream1.getIndexSet(), never()).isManagedIndex(any());
        verify(stream2.getIndexSet(), never()).isManagedIndex(any());
        verify(indexRange, never()).indexName();
    }

    @Test
    public void closedIndexRangeShouldNotMatchIfNotContainingAnyStreamId() {
        when(indexRange.streamIds()).thenReturn(Collections.emptyList());
        when(stream1.getId()).thenReturn("stream1");
        when(stream2.getId()).thenReturn("stream2");

        final IndexRangeContainsOneOfStreams predicate = new IndexRangeContainsOneOfStreams(stream1, stream2);

        assertThat(predicate.test(indexRange)).isFalse();

        verify(stream1.getIndexSet(), never()).isManagedIndex(any());
        verify(stream2.getIndexSet(), never()).isManagedIndex(any());
        verify(indexRange, never()).indexName();
    }
}
