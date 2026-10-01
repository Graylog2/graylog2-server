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
package org.graylog.collectors;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.collectors.events.CollectorsConfigUpdatedEvent;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectorsConfigServiceTest {

    private ClusterConfigService clusterConfigService;
    private EventBus eventBus;
    private CollectorsConfigService service;
    private UpdatedEventCollector updatedEvents;

    private static final class UpdatedEventCollector {
        private final List<CollectorsConfigUpdatedEvent> events = new ArrayList<>();

        @Subscribe
        public void handleUpdated(CollectorsConfigUpdatedEvent event) {
            events.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        clusterConfigService = mock(ClusterConfigService.class);
        eventBus = new EventBus();
        final var httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(java.net.URI.create("https://localhost:443/"));
        service = new CollectorsConfigService(clusterConfigService, httpConfiguration, eventBus);
        updatedEvents = new UpdatedEventCollector();
        eventBus.register(updatedEvents);
    }

    private CollectorsConfig configWithPort(int port) {
        return CollectorsConfig.createDefaultBuilder("localhost")
                .http(new IngestEndpointConfig("localhost", port))
                .build();
    }

    private void postClusterConfigChangedEvent(String type) {
        eventBus.post(ClusterConfigChangedEvent.create(DateTime.now(DateTimeZone.UTC), "node-id", type));
    }

    @Test
    void get_cachesLoadedConfig() {
        final var config = configWithPort(1111);
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(config);

        assertThat(service.get()).contains(config);
        assertThat(service.get()).contains(config);

        verify(clusterConfigService, times(1)).get(CollectorsConfig.class);
    }

    @Test
    void get_cachesEmptyResult() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        assertThat(service.get()).isEmpty();
        assertThat(service.get()).isEmpty();

        verify(clusterConfigService, times(1)).get(CollectorsConfig.class);
    }

    @Test
    void save_writesConfigAndInvalidatesCache() {
        final var initial = configWithPort(1111);
        final var updated = configWithPort(2222);
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(initial, updated);

        assertThat(service.get()).contains(initial);

        service.save(updated);

        verify(clusterConfigService).write(updated);
        assertThat(service.get()).contains(updated);
    }

    @Test
    void clusterConfigChangedEvent_invalidatesCacheAndPostsUpdatedEvent() {
        final var initial = configWithPort(1111);
        final var updated = configWithPort(2222);
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(initial, updated);

        assertThat(service.get()).contains(initial);

        postClusterConfigChangedEvent(CollectorsConfig.class.getCanonicalName());

        assertThat(service.get()).contains(updated);
        assertThat(updatedEvents.events).hasSize(1);
    }

    @Test
    void clusterConfigChangedEventForOtherType_isIgnored() {
        final var initial = configWithPort(1111);
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(initial, configWithPort(2222));

        assertThat(service.get()).contains(initial);

        postClusterConfigChangedEvent("org.graylog2.some.OtherConfig");

        assertThat(service.get()).contains(initial);
        verify(clusterConfigService, times(1)).get(CollectorsConfig.class);
        assertThat(updatedEvents.events).isEmpty();
    }
}
