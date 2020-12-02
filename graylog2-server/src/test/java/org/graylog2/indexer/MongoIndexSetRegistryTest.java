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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indexset.events.IndexSetDeletedEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MongoIndexSetRegistryTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private MongoIndexSetRegistry indexSetRegistry;
    private MongoIndexSetRegistry.IndexSetsCache indexSetsCache;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private MongoIndexSet.Factory mongoIndexSetFactory;
    @Mock
    private EventBus serverEventBus;

    @Before
    public void setUp() throws Exception {
        this.indexSetsCache = new MongoIndexSetRegistry.IndexSetsCache(indexSetService, serverEventBus);
        this.indexSetRegistry = new MongoIndexSetRegistry(indexSetService, mongoIndexSetFactory, indexSetsCache);
    }

    @Test
    public void indexSetsCacheShouldReturnCachedList() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);

        final List<IndexSetConfig> result = this.indexSetsCache.get();
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .containsExactly(indexSetConfig);

        final List<IndexSetConfig> cachedResult = this.indexSetsCache.get();
        assertThat(cachedResult)
            .isNotNull()
            .hasSize(1)
            .containsExactly(indexSetConfig);

        verify(indexSetService, times(1)).findAll();
    }

    @Test
    public void indexSetsCacheShouldReturnNewListAfterInvalidate() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);

        final List<IndexSetConfig> result = this.indexSetsCache.get();
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .containsExactly(indexSetConfig);

        this.indexSetsCache.invalidate();

        final IndexSetConfig newIndexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> newIndexSetConfigs = Collections.singletonList(newIndexSetConfig);
        when(indexSetService.findAll()).thenReturn(newIndexSetConfigs);


        final List<IndexSetConfig> newResult = this.indexSetsCache.get();
        assertThat(newResult)
            .isNotNull()
            .hasSize(1)
            .containsExactly(newIndexSetConfig);

        verify(indexSetService, times(2)).findAll();
    }

    @Test
    public void indexSetsCacheShouldBeInvalidatedForIndexSetCreation() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);

        final List<IndexSetConfig> result = this.indexSetsCache.get();
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .containsExactly(indexSetConfig);

        this.indexSetsCache.handleIndexSetCreation(mock(IndexSetCreatedEvent.class));

        final IndexSetConfig newIndexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> newIndexSetConfigs = Collections.singletonList(newIndexSetConfig);
        when(indexSetService.findAll()).thenReturn(newIndexSetConfigs);


        final List<IndexSetConfig> newResult = this.indexSetsCache.get();
        assertThat(newResult)
            .isNotNull()
            .hasSize(1)
            .containsExactly(newIndexSetConfig);

        verify(indexSetService, times(2)).findAll();
    }

    @Test
    public void indexSetsCacheShouldBeInvalidatedForIndexSetDeletion() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);

        final List<IndexSetConfig> result = this.indexSetsCache.get();
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .containsExactly(indexSetConfig);

        this.indexSetsCache.handleIndexSetDeletion(mock(IndexSetDeletedEvent.class));

        final IndexSetConfig newIndexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> newIndexSetConfigs = Collections.singletonList(newIndexSetConfig);
        when(indexSetService.findAll()).thenReturn(newIndexSetConfigs);


        final List<IndexSetConfig> newResult = this.indexSetsCache.get();
        assertThat(newResult)
            .isNotNull()
            .hasSize(1)
            .containsExactly(newIndexSetConfig);

        verify(indexSetService, times(2)).findAll();
    }

    @Test
    public void getAllShouldBeCachedForEmptyList() {
        final List<IndexSetConfig> indexSetConfigs = Collections.emptyList();
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);

        assertThat(this.indexSetRegistry.getAll())
            .isNotNull()
            .isEmpty();

        assertThat(this.indexSetRegistry.getAll())
            .isNotNull()
            .isEmpty();

        verify(indexSetService, times(1)).findAll();
    }

    @Test
    public void getAllShouldBeCachedForNonEmptyList() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        final MongoIndexSet indexSet = mock(MongoIndexSet.class);
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(indexSet);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);

        assertThat(this.indexSetRegistry.getAll())
            .isNotNull()
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(indexSet);

        assertThat(this.indexSetRegistry.getAll())
            .isNotNull()
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(indexSet);

        verify(indexSetService, times(1)).findAll();
    }

    @Test
    public void getAllShouldNotBeCachedForCallAfterInvalidate() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        final MongoIndexSet indexSet = mock(MongoIndexSet.class);
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(indexSet);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);

        assertThat(this.indexSetRegistry.getAll())
            .isNotNull()
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(indexSet);

        this.indexSetsCache.invalidate();

        assertThat(this.indexSetRegistry.getAll())
            .isNotNull()
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(indexSet);

        verify(indexSetService, times(2)).findAll();
    }

    @Test
    public void isManagedIndexReturnsAMapOfIndices() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        final MongoIndexSet indexSet = mock(MongoIndexSet.class);
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(indexSet);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);
        when(indexSet.isManagedIndex("index1")).thenReturn(true);
        when(indexSet.isManagedIndex("index2")).thenReturn(false);

        final Map<String, Boolean> managedStatus = indexSetRegistry.isManagedIndex(ImmutableSet.of("index1", "index2"));
        assertThat(managedStatus)
                .containsEntry("index1", true)
                .containsEntry("index2", false);
    }

    @Test
    public void isManagedIndexWithManagedIndexReturnsTrue() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        final MongoIndexSet indexSet = mock(MongoIndexSet.class);
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(indexSet);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);
        when(indexSet.isManagedIndex("index")).thenReturn(true);

        assertThat(indexSetRegistry.isManagedIndex("index")).isTrue();
    }

    @Test
    public void isManagedIndexWithUnmanagedIndexReturnsFalse() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        final List<IndexSetConfig> indexSetConfigs = Collections.singletonList(indexSetConfig);
        final MongoIndexSet indexSet = mock(MongoIndexSet.class);
        when(mongoIndexSetFactory.create(indexSetConfig)).thenReturn(indexSet);
        when(indexSetService.findAll()).thenReturn(indexSetConfigs);
        when(indexSet.isManagedIndex("index")).thenReturn(false);

        assertThat(indexSetRegistry.isManagedIndex("index")).isFalse();
    }
}
