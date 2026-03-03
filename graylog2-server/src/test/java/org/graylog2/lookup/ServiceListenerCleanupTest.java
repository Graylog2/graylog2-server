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
package org.graylog2.lookup;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Service;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ServiceListenerCleanupTest {

    @Mock
    private ScheduledExecutorService scheduler;
    @Mock
    private EventBus eventBus;
    @Mock
    private LookupTableConfigService configService;
    @Mock
    private Consumer<LookupDataAdapter> adapterConsumer;
    @Mock
    private Consumer<LookupCache> cacheConsumer;

    private LookupTableService service;

    @BeforeEach
    void setUp() {
        service = new LookupTableService(
                configService, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                scheduler, eventBus);
    }

    //=============================
    // Testing DataAdapterListener:
    //=============================

    @Test
    void replacedAdapterConsumerIsUnsetAfterRunning() {
        final var dto = DataAdapterDto.builder()
                .id("id").name("name").title("t").description("d")
                .config(new FallbackAdapterConfig()).build();

        final var adapter = mock(LookupDataAdapter.class);
        final var latch = new CountDownLatch(1);

        final var listener = service.new DataAdapterListener(dto, adapter, latch, adapterConsumer);
        listener.running();

        assertThat(listener.isReplacedAdapterConsumerSet()).isFalse();
    }

    @Test
    void replacedAdapterConsumerIsUnsetAfterFailed() {
        final var dto = DataAdapterDto.builder()
                .id("id").name("name").title("t").description("d")
                .config(new FallbackAdapterConfig()).build();

        final var adapter = mock(LookupDataAdapter.class);
        final var latch = new CountDownLatch(1);

        final var listener = service.new DataAdapterListener(dto, adapter, latch, adapterConsumer);

        listener.failed(Service.State.STARTING, new RuntimeException("boom"));

        assertThat(listener.isReplacedAdapterConsumerSet()).isFalse();
    }

    //=======================
    // Testing CacheListener:
    //=======================

    @Test
    void replacedCacheConsumerIsUnsetAfterRunning() {
        final var dto = CacheDto.builder()
                .id("id").name("name").title("t").description("d")
                .config(new FallbackCacheConfig()).build();

        final var cache = mock(LookupCache.class);
        final var latch = new CountDownLatch(1);

        final var listener = service.new CacheListener(dto, cache, latch, cacheConsumer);
        listener.running();

        assertThat(listener.isReplacedCacheConsumerSet()).isFalse();
    }

    @Test
    void replacedCacheConsumerIsUnsetAfterFailed() {
        final var dto = CacheDto.builder()
                .id("id").name("name").title("t").description("d")
                .config(new FallbackCacheConfig()).build();

        final var cache = mock(LookupCache.class);
        final var latch = new CountDownLatch(1);

        final var listener = service.new CacheListener(dto, cache, latch, cacheConsumer);

        listener.failed(Service.State.STARTING, new RuntimeException("boom"));

        assertThat(listener.isReplacedCacheConsumerSet()).isFalse();
    }
}
