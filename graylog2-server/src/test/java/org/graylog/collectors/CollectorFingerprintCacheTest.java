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
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.events.CollectorInstanceCertsChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorFingerprintCacheTest {

    @Mock
    private CollectorsConfigService configService;
    @Mock
    private CollectorInstanceService instanceService;

    private EventBus eventBus;
    private CollectorFingerprintCache cache;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        // Runnable::run makes Caffeine's refresh reloads (and preWarm) run inline for deterministic tests.
        cache = new CollectorFingerprintCache(configService, instanceService, eventBus, Runnable::run);
    }

    @Test
    void lookupReturnsInstanceUidAndCachesIt() {
        final var instance = instance("uid-1");
        when(instanceService.findByActiveOrNextFingerprint("fp-1")).thenReturn(Optional.of(instance));

        assertThat(cache.lookup("fp-1")).contains("uid-1");
        assertThat(cache.lookup("fp-1")).contains("uid-1");

        verify(instanceService, times(1)).findByActiveOrNextFingerprint("fp-1");
    }

    @Test
    void lookupCachesNegativeResult() {
        when(instanceService.findByActiveOrNextFingerprint("fp-unknown")).thenReturn(Optional.empty());

        assertThat(cache.lookup("fp-unknown")).isEmpty();
        assertThat(cache.lookup("fp-unknown")).isEmpty();

        verify(instanceService, times(1)).findByActiveOrNextFingerprint("fp-unknown");
    }

    @Test
    void lookupFailsClosedWhenLoaderThrows() {
        when(instanceService.findByActiveOrNextFingerprint("fp-err")).thenThrow(new RuntimeException("mongo down"));

        assertThat(cache.lookup("fp-err")).isEmpty();
    }

    @Test
    void removedFingerprintIsInvalidatedAndReResolved() {
        final var instance = instance("uid-1");
        when(instanceService.findByActiveOrNextFingerprint("fp-1")).thenReturn(Optional.of(instance));
        assertThat(cache.lookup("fp-1")).contains("uid-1");

        // The instance is deleted: the fingerprint no longer resolves.
        when(instanceService.findByActiveOrNextFingerprint("fp-1")).thenReturn(Optional.empty());
        cache.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of(), Set.of("fp-1")));

        assertThat(cache.lookup("fp-1")).isEmpty();
        verify(instanceService, times(2)).findByActiveOrNextFingerprint("fp-1");
    }

    @Test
    void addedFingerprintIsRefreshedIntoCache() {
        final var instance = instance("uid-2");
        when(instanceService.findByActiveOrNextFingerprint("fp-2")).thenReturn(Optional.of(instance));

        cache.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-2"), Set.of()));

        // The refresh loaded fp-2, so the subsequent lookup is a cache hit (loader called once, by the refresh).
        assertThat(cache.lookup("fp-2")).contains("uid-2");
        verify(instanceService, times(1)).findByActiveOrNextFingerprint("fp-2");
    }

    @Test
    void preWarmLoadsUnexpiredInstances() {
        final var config = CollectorsConfig.createDefault("test-host");
        when(configService.getOrDefault()).thenReturn(config);
        final var instance = instanceWithCerts("uid-pw", "active-fp", "next-fp");
        when(instanceService.streamAllUnexpired(config.collectorExpirationThreshold())).thenReturn(Stream.of(instance));

        cache.startAsync().awaitRunning();
        try {
            assertThat(cache.lookup("active-fp")).contains("uid-pw");
            assertThat(cache.lookup("next-fp")).contains("uid-pw");
            // Both fingerprints were prewarmed, so no per-fingerprint lookup hit MongoDB.
            verify(instanceService, never()).findByActiveOrNextFingerprint(any());
        } finally {
            cache.stopAsync().awaitTerminated();
        }
    }

    @Test
    void certsChangedEventPostedToEventBusIsHandled() {
        final var config = CollectorsConfig.createDefault("test-host");
        when(configService.getOrDefault()).thenReturn(config);
        when(instanceService.streamAllUnexpired(any())).thenReturn(Stream.empty());

        cache.startAsync().awaitRunning();
        try {
            final var instance = instance("uid-1");
            when(instanceService.findByActiveOrNextFingerprint("fp-1")).thenReturn(Optional.of(instance));
            assertThat(cache.lookup("fp-1")).contains("uid-1");

            when(instanceService.findByActiveOrNextFingerprint("fp-1")).thenReturn(Optional.empty());
            eventBus.post(new CollectorInstanceCertsChangedEvent(Set.of(), Set.of("fp-1")));

            assertThat(cache.lookup("fp-1")).isEmpty();
        } finally {
            cache.stopAsync().awaitTerminated();
        }
    }

    private static CollectorInstanceDTO instance(String instanceUid) {
        final var dto = mock(CollectorInstanceDTO.class);
        when(dto.instanceUid()).thenReturn(instanceUid);
        return dto;
    }

    private static CollectorInstanceDTO instanceWithCerts(String instanceUid, String activeFp, String nextFp) {
        final var dto = mock(CollectorInstanceDTO.class);
        when(dto.instanceUid()).thenReturn(instanceUid);
        when(dto.activeCertificateFingerprint()).thenReturn(activeFp);
        when(dto.nextCertificateFingerprint()).thenReturn(Optional.ofNullable(nextFp));
        return dto;
    }
}
