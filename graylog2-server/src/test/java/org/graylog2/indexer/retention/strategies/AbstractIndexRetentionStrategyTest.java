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
package org.graylog2.indexer.retention.strategies;

import org.graylog.scheduler.clock.JobSchedulerSystemClock;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractIndexRetentionStrategyTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private AbstractIndexRetentionStrategy retentionStrategy;
    @Mock
    private Indices indices;
    @Mock
    private ActivityWriter activityWriter;
    @Mock
    private IndexSet indexSet;

    private Map<String, Set<String>> indexMap;

    private IndexSetConfig indexSetConfigCountBased;

    private DateTime NOW = DateTime.now(DateTimeZone.UTC);

    @Before
    public void setUp() throws Exception {
        indexMap = new HashMap<>();
        indexMap.put("index1", Collections.emptySet());
        indexMap.put("index2", Collections.emptySet());
        indexMap.put("index3", Collections.emptySet());
        indexMap.put("index4", Collections.emptySet());
        indexMap.put("index5", Collections.emptySet());
        indexMap.put("index6", Collections.emptySet());

        lenient().when(indices.indexClosingDate("index6")).thenReturn(Optional.of(NOW.minusDays(1)));
        lenient().when(indices.indexClosingDate("index5")).thenReturn(Optional.of(NOW.minusDays(3)));
        lenient().when(indices.indexClosingDate("index4")).thenReturn(Optional.of(NOW.minusDays(9)));
        lenient().when(indices.indexClosingDate("index3")).thenReturn(Optional.of(NOW.minusDays(10)));
        lenient().when(indices.indexClosingDate("index2")).thenReturn(Optional.of(NOW.minusDays(11)));
        lenient().when(indices.indexClosingDate("index1")).thenReturn(Optional.of(NOW.minusDays(15)));

        indexSetConfigCountBased = createCountBased();

        when(indexSet.getAllIndexAliases()).thenReturn(indexMap);
        lenient().when(indexSet.getManagedIndices()).thenReturn(indexMap.keySet().stream().toArray(String[]::new));
        lenient().when(indexSet.extractIndexNumber(anyString())).then(this::extractIndexNumber);
        when(indexSet.getConfig()).thenReturn(indexSetConfigCountBased);

        // Report all but the newest index as read-only
        lenient().when(indices.getIndicesBlocksStatus(anyList())).then(a -> {
            final List<String> indices = a.getArgument(0);
            final IndicesBlockStatus indicesBlockStatus = new IndicesBlockStatus();
            final String newestIndex = "index6";
            indices.forEach(i -> {
                if (!newestIndex.equals(i)) {
                    indicesBlockStatus.addIndexBlocks(i, Set.of("index.blocks.write"));
                }
            });
            return indicesBlockStatus;
        });

        retentionStrategy = spy(new AbstractIndexRetentionStrategy(indices, activityWriter, new JobSchedulerSystemClock()) {
            @Override
            protected Optional<Integer> getMaxNumberOfIndices(IndexSet indexSet) {
                return null;
            }

            @Override
            protected void retain(List<String> indexName, IndexSet indexSet) {

            }

            @Override
            public Class<? extends RetentionStrategyConfig> configurationClass() {
                return null;
            }

            @Override
            public RetentionStrategyConfig defaultConfiguration() {
                return null;
            }
        });

        lenient().when(retentionStrategy.getMaxNumberOfIndices(eq(indexSet))).thenReturn(Optional.of(5));
        when(indices.isReopened(anyString())).thenReturn(false);
    }

    @Test
    public void shouldRetainOldestIndex() throws Exception {
        retentionStrategy.retain(indexSet);

        final ArgumentCaptor<List> retainedIndexName = ArgumentCaptor.forClass(List.class);
        verify(retentionStrategy, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index1");

        verify(activityWriter, times(2)).write(any(Activity.class));
    }

    @Test
    public void shouldRetainOldestIndices() throws Exception {
        when(retentionStrategy.getMaxNumberOfIndices(eq(indexSet))).thenReturn(Optional.of(4));

        retentionStrategy.retain(indexSet);

        final ArgumentCaptor<List> retainedIndexName = ArgumentCaptor.forClass(List.class);
        verify(retentionStrategy, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        // Ensure that the oldest indices come first
        assertThat(retainedIndexName.getAllValues().get(0)).containsExactly("index1", "index2");

        verify(activityWriter, times(2)).write(any(Activity.class));
    }

    @Test
    public void shouldIgnoreReopenedIndexWhenCountingAgainstLimit() {
        when(indices.isReopened(eq("index1"))).thenReturn(true);

        retentionStrategy.retain(indexSet);

        verify(retentionStrategy, never()).retain(any(List.class), eq(indexSet));

        verify(activityWriter, never()).write(any(Activity.class));
    }

    @Test
    public void shouldIgnoreReopenedIndexWhenDeterminingRetainedIndices() {
        when(retentionStrategy.getMaxNumberOfIndices(eq(indexSet))).thenReturn(Optional.of(4));
        when(indices.isReopened(eq("index1"))).thenReturn(true);

        retentionStrategy.retain(indexSet);

        final ArgumentCaptor<List> retainedIndexName = ArgumentCaptor.forClass(List.class);
        verify(retentionStrategy, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index2");

        verify(activityWriter, times(2)).write(any(Activity.class));
    }

    @Test
    public void shouldIgnoreWriteAliasWhenDeterminingRetainedIndices() {
        final String indexWithWriteIndexAlias = "index1";
        final String writeIndexAlias = "WriteIndexAlias";

        when(indexSet.getWriteIndexAlias()).thenReturn(writeIndexAlias);
        indexMap.put(indexWithWriteIndexAlias, Collections.singleton(writeIndexAlias));

        retentionStrategy.retain(indexSet);

        final ArgumentCaptor<List> retainedIndexName = ArgumentCaptor.forClass(List.class);
        verify(retentionStrategy, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index2");

        verify(activityWriter, times(2)).write(any(Activity.class));
    }

    @Test
    public void retainTimeBased() {
        when(indexSet.getConfig()).thenReturn(createTimeBased(10, 12));

        retentionStrategy.retain(indexSet);

        final ArgumentCaptor<List> retainedIndexName = ArgumentCaptor.forClass(List.class);
        verify(retentionStrategy, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index1", "index2", "index3");
    }

    @Test
    public void retainTimeBasedNothing() {
        when(indexSet.getConfig()).thenReturn(createTimeBased(20, 30));

        retentionStrategy.retain(indexSet);

        verify(retentionStrategy, times(0)).retain(any(), any());
    }

    @Test
    public void timeBasedMissingClosingDate() {
        when(indexSet.getConfig()).thenReturn(createTimeBased(14, 16));
        when(indices.indexClosingDate("index1")).thenReturn(Optional.empty());
        when(indices.indexCreationDate("index1")).thenReturn(Optional.of(NOW.minusDays(17)));

        retentionStrategy.retain(indexSet);

        final ArgumentCaptor<List> retainedIndexName = ArgumentCaptor.forClass(List.class);
        verify(retentionStrategy, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index1");
    }

    @Test
    public void timeBasedNoDates() {
        when(indexSet.getConfig()).thenReturn(createTimeBased(14, 16));
        when(indices.indexClosingDate("index1")).thenReturn(Optional.empty());
        when(indices.indexCreationDate("index1")).thenReturn(Optional.empty());

        retentionStrategy.retain(indexSet);

        final ArgumentCaptor<List> retainedIndexName = ArgumentCaptor.forClass(List.class);
        verify(retentionStrategy, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index1");
    }

    private Optional<Integer> extractIndexNumber(InvocationOnMock invocation) {
        return Optional.of(Integer.parseInt(((String) invocation.getArgument(0)).replace("index", "")));
    }

    private IndexSetConfig createCountBased() {
        return IndexSetConfig.create(
                "id", "title", "description",
                true,
                true, "prefix", null, null,
                1, 0,
                MessageCountRotationStrategyConfig.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(3),
                DeletionRetentionStrategy.class.getCanonicalName(),
                DeletionRetentionStrategyConfig.createDefault(),
                ZonedDateTime.now(ZoneId.systemDefault()),
                null, null, null,
                1, true,
                Duration.standardSeconds(5),
                new CustomFieldMappings(),
                null);
    }

    private IndexSetConfig createTimeBased(int minDays, int maxDays) {
        return IndexSetConfig.create(
                "id", "title", "description",
                true,
                true, "prefix", null, null,
                1, 0,
                TimeBasedSizeOptimizingStrategyConfig.class.getCanonicalName(),
                TimeBasedSizeOptimizingStrategyConfig.builder()
                        .indexLifetimeMin(Period.days(minDays))
                        .indexLifetimeMax(Period.days(maxDays))
                        .build(),
                DeletionRetentionStrategy.class.getCanonicalName(),
                DeletionRetentionStrategyConfig.createDefault(),
                ZonedDateTime.now(ZoneId.systemDefault()),
                null, null, null,
                1, true,
                Duration.standardSeconds(5),
                new CustomFieldMappings(),
                null);
    }
}
