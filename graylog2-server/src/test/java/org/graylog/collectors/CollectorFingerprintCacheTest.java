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
import org.threeten.extra.MutableClock;

import java.time.Duration;
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

/**
 * Tests the caching mechanics over {@link CollectorInstanceService#resolveCertBinding(String)}: what is
 * cached, for how long, how events refresh it, and that reads enforce the binding's {@code validUntil}
 * stamp locally (no reload). How the stamp is computed (cert expiry, renewal grace) is the service's
 * responsibility and is tested in the service's own test — here {@code resolveCertBinding} is mocked.
 */
@ExtendWith(MockitoExtension.class)
class CollectorFingerprintCacheTest {

    @Mock
    private CollectorsConfigService configService;
    @Mock
    private CollectorInstanceService instanceService;

    private EventBus eventBus;
    private MutableClock clock;
    private CollectorFingerprintCache cache;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        clock = MutableClock.epochUTC();
        // Runnable::run makes Caffeine's refresh reloads (and preWarm) run inline for deterministic tests.
        // The ticker is derived from this clock, so advancing it drives the idle expiry.
        cache = new CollectorFingerprintCache(configService, instanceService, eventBus, clock, Runnable::run);
    }

    /**
     * A binding whose stamp is far in the future, for tests that are not about validity.
     */
    private CertBinding longLivedBinding(String instanceUid) {
        return new CertBinding(instanceUid, clock.instant().plus(Duration.ofDays(365)));
    }

    @Test
    void lookupReturnsInstanceUidAndCachesIt() {
        when(instanceService.resolveCertBinding("fp-1")).thenReturn(Optional.of(longLivedBinding("uid-1")));

        assertThat(cache.lookup("fp-1")).contains("uid-1");
        assertThat(cache.lookup("fp-1")).contains("uid-1");

        verify(instanceService, times(1)).resolveCertBinding("fp-1");
    }

    @Test
    void lookupCachesNegativeResult() {
        when(instanceService.resolveCertBinding("fp-unknown")).thenReturn(Optional.empty());

        assertThat(cache.lookup("fp-unknown")).isEmpty();
        assertThat(cache.lookup("fp-unknown")).isEmpty();

        verify(instanceService, times(1)).resolveCertBinding("fp-unknown");
    }

    @Test
    void lookupFailsClosedWhenResolveThrows() {
        when(instanceService.resolveCertBinding("fp-err")).thenThrow(new RuntimeException("mongo down"));

        assertThat(cache.lookup("fp-err")).isEmpty();
    }

    @Test
    void lookupRejectsExpiredBindingWithoutReload() {
        final var validUntil = clock.instant().plus(Duration.ofMinutes(5));
        when(instanceService.resolveCertBinding("prev-fp"))
                .thenReturn(Optional.of(new CertBinding("uid-prev", validUntil)));

        assertThat(cache.lookup("prev-fp")).contains("uid-prev");

        clock.add(Duration.ofMinutes(4)); // still within the stamp
        assertThat(cache.lookup("prev-fp")).contains("uid-prev");

        // Past the stamp the still-cached entry keeps serving local rejects — the cut must not cost a
        // reload (this is what keeps the deadline cut off the Netty event loop).
        clock.add(Duration.ofMinutes(2)); // now past validUntil
        assertThat(cache.lookup("prev-fp")).isEmpty();
        assertThat(cache.lookup("prev-fp")).isEmpty();

        verify(instanceService, times(1)).resolveCertBinding("prev-fp");
    }

    @Test
    void idleEntryExpiresAndReloadsOnNextLookup() {
        when(instanceService.resolveCertBinding("fp-idle")).thenReturn(Optional.of(longLivedBinding("uid-idle")));

        assertThat(cache.lookup("fp-idle")).contains("uid-idle");

        clock.add(Duration.ofMinutes(61)); // exceeds IDLE_EXPIRY without any access
        assertThat(cache.lookup("fp-idle")).contains("uid-idle");

        verify(instanceService, times(2)).resolveCertBinding("fp-idle");
    }

    @Test
    void touchedFingerprintThatIsCachedIsReResolved() {
        when(instanceService.resolveCertBinding("fp-1")).thenReturn(Optional.of(longLivedBinding("uid-1")));
        assertThat(cache.lookup("fp-1")).contains("uid-1");

        // The instance is deleted: the fingerprint no longer binds. A certs-changed event touching the
        // still-cached fingerprint re-resolves it (to unbound).
        when(instanceService.resolveCertBinding("fp-1")).thenReturn(Optional.empty());
        cache.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-1")));

        assertThat(cache.lookup("fp-1")).isEmpty();
        verify(instanceService, times(2)).resolveCertBinding("fp-1");
    }

    @Test
    void refreshInstallsGraceStampOnLiveEntry() {
        // Rotation scenario: the fingerprint is cached as a plain active binding, then rotation demotes it
        // to the previous slot — the certs-changed refresh must install the grace stamp into the live
        // entry, and the subsequent cut must again be reload-free.
        when(instanceService.resolveCertBinding("fp-rot")).thenReturn(Optional.of(longLivedBinding("uid-rot")));
        assertThat(cache.lookup("fp-rot")).contains("uid-rot");

        final var graceDeadline = clock.instant().plus(Duration.ofMinutes(5));
        when(instanceService.resolveCertBinding("fp-rot"))
                .thenReturn(Optional.of(new CertBinding("uid-rot", graceDeadline)));
        cache.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-rot")));

        assertThat(cache.lookup("fp-rot")).contains("uid-rot"); // still within grace

        clock.add(Duration.ofMinutes(6)); // past the grace deadline
        assertThat(cache.lookup("fp-rot")).isEmpty();
        verify(instanceService, times(2)).resolveCertBinding("fp-rot"); // initial load + refresh, no third
    }

    @Test
    void touchedFingerprintThatIsNotCachedIsNotLoaded() {
        when(instanceService.resolveCertBinding("fp-2")).thenReturn(Optional.of(longLivedBinding("uid-2")));

        // fp-2 is not cached, so a certs-changed event must NOT proactively load it (avoids loading the
        // many fingerprints of expired instances that no longer exist).
        cache.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-2")));
        verify(instanceService, never()).resolveCertBinding("fp-2");

        // It cold-loads on first lookup instead.
        assertThat(cache.lookup("fp-2")).contains("uid-2");
        verify(instanceService, times(1)).resolveCertBinding("fp-2");
    }

    @Test
    void preWarmLoadsOnlyActiveFingerprints() {
        final var config = CollectorsConfig.createDefault("test-host");
        when(configService.getOrDefault()).thenReturn(config);
        final var instance = mock(CollectorInstanceDTO.class);
        when(instance.instanceUid()).thenReturn("uid-pw");
        when(instance.activeCertificateFingerprint()).thenReturn("active-fp");
        when(instance.activeCertificateExpiresAt()).thenReturn(clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.streamAllUnexpired(config.collectorExpirationThreshold())).thenReturn(Stream.of(instance));

        cache.startAsync().awaitRunning();
        try {
            // The active fingerprint is prewarmed: it resolves without consulting the service.
            assertThat(cache.lookup("active-fp")).contains("uid-pw");
            verify(instanceService, never()).resolveCertBinding(any());

            // The next fingerprint is NOT prewarmed — it cold-loads on first lookup.
            when(instanceService.resolveCertBinding("next-fp")).thenReturn(Optional.of(longLivedBinding("uid-pw")));
            assertThat(cache.lookup("next-fp")).contains("uid-pw");
            verify(instanceService).resolveCertBinding("next-fp");
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
            when(instanceService.resolveCertBinding("fp-1")).thenReturn(Optional.of(longLivedBinding("uid-1")));
            assertThat(cache.lookup("fp-1")).contains("uid-1");

            when(instanceService.resolveCertBinding("fp-1")).thenReturn(Optional.empty());
            eventBus.post(new CollectorInstanceCertsChangedEvent(Set.of("fp-1")));

            assertThat(cache.lookup("fp-1")).isEmpty();
        } finally {
            cache.stopAsync().awaitTerminated();
        }
    }
}
