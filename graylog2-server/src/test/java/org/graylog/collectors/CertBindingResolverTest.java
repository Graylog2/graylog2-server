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
import java.time.Instant;
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
 * Tests the binding resolution (which certificate slot binds, until when — cert expiry, rotation grace)
 * and the caching mechanics around it (what is cached, for how long, how events refresh it, and that
 * reads enforce validity locally without a reload). The instance service is mocked at the raw document
 * lookup, so no MongoDB is involved.
 */
@ExtendWith(MockitoExtension.class)
class CertBindingResolverTest {

    private static final Duration GRACE_PERIOD = Duration.ofMinutes(5);

    @Mock
    private CollectorsConfigService configService;
    @Mock
    private CollectorInstanceService instanceService;

    private EventBus eventBus;
    private MutableClock clock;
    private CertBindingResolver resolver;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        clock = MutableClock.epochUTC();
        // Runnable::run makes Caffeine's refresh reloads (and preWarm) run inline for deterministic tests.
        // The ticker is derived from this clock, so advancing it drives the idle expiry.
        resolver = new CertBindingResolver(configService, instanceService, eventBus, clock, Runnable::run);
    }

    // ----- Binding resolution: which slot binds, until when -----

    @Test
    void resolvesActiveFingerprintUntilCertExpiry() {
        final var instance = activeInstance("uid-1", "fp-active", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-active"))
                .thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-active")).contains("uid-1");
    }

    @Test
    void rejectsActiveFingerprintOfExpiredCert() {
        // An expired certificate must not resolve even though it still occupies the active slot — the
        // per-request resolution is the only post-handshake enforcement of cert expiry (TLS never
        // re-validates an established connection).
        final var instance = activeInstance("uid-1", "fp-active", clock.instant().minus(Duration.ofDays(1)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-active"))
                .thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-active")).isEmpty();
    }

    @Test
    void resolvesNextFingerprintUntilCertExpiry() {
        final var instance = nextInstance("uid-1", "fp-next", clock.instant().plus(Duration.ofDays(30)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-next"))
                .thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-next")).contains("uid-1");
    }

    @Test
    void resolvesPreviousFingerprintOnlyWithinGraceWindow() {
        stubGracePeriod(GRACE_PERIOD);
        final var instance = previousInstance("uid-1", "fp-prev", clock.instant(),
                clock.instant().plus(Duration.ofDays(90)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-prev"))
                .thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-prev")).contains("uid-1"); // within grace

        clock.add(GRACE_PERIOD.plus(Duration.ofMinutes(1)));
        assertThat(resolver.resolve("fp-prev")).isEmpty(); // past grace
    }

    @Test
    void previousBindingIsCappedByCertExpiry() {
        // The superseded cert expires before the grace window ends: the earlier instant wins.
        stubGracePeriod(GRACE_PERIOD);
        final var instance = previousInstance("uid-1", "fp-prev", clock.instant(),
                clock.instant().plus(Duration.ofMinutes(1)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-prev"))
                .thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-prev")).contains("uid-1");

        clock.add(Duration.ofMinutes(2)); // past cert expiry, still within grace
        assertThat(resolver.resolve("fp-prev")).isEmpty();
    }

    @Test
    void zeroGracePeriodCutsSupersededCertImmediately() {
        // grace = 0 restores prompt-cut behavior: the binding expires at the rotation instant.
        stubGracePeriod(Duration.ZERO);
        final var instance = previousInstance("uid-1", "fp-prev", clock.instant(),
                clock.instant().plus(Duration.ofDays(90)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-prev"))
                .thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-prev")).isEmpty();
    }

    @Test
    void previousFingerprintWithoutRotationTimestampDoesNotResolve() {
        // A missing rotation timestamp (shouldn't happen) must not grant an unbounded grace.
        stubGracePeriod(GRACE_PERIOD);
        final var instance = previousInstance("uid-1", "fp-prev", null,
                clock.instant().plus(Duration.ofDays(90)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-prev"))
                .thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-prev")).isEmpty();
    }

    @Test
    void unknownFingerprintDoesNotResolve() {
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-unknown"))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolve("fp-unknown")).isEmpty();
    }

    // ----- Caching mechanics -----

    @Test
    void resolveCachesPositiveResult() {
        final var instance = activeInstance("uid-1", "fp-1", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1")).thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-1")).contains("uid-1");
        assertThat(resolver.resolve("fp-1")).contains("uid-1");

        verify(instanceService, times(1)).findByActiveOrNextOrPreviousFingerprint("fp-1");
    }

    @Test
    void resolveCachesNegativeResult() {
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-unknown")).thenReturn(Optional.empty());

        assertThat(resolver.resolve("fp-unknown")).isEmpty();
        assertThat(resolver.resolve("fp-unknown")).isEmpty();

        verify(instanceService, times(1)).findByActiveOrNextOrPreviousFingerprint("fp-unknown");
    }

    @Test
    void resolveFailsClosedWhenLoadThrows() {
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-err"))
                .thenThrow(new RuntimeException("mongo down"));

        assertThat(resolver.resolve("fp-err")).isEmpty();
    }

    @Test
    void resolveRejectsExpiredBindingWithoutReload() {
        stubGracePeriod(GRACE_PERIOD);
        final var instance = previousInstance("uid-prev", "prev-fp", clock.instant(),
                clock.instant().plus(Duration.ofDays(90)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("prev-fp")).thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("prev-fp")).contains("uid-prev");

        clock.add(Duration.ofMinutes(4)); // still within grace
        assertThat(resolver.resolve("prev-fp")).contains("uid-prev");

        // Past the grace deadline the still-cached entry keeps serving local rejects — the cut must not
        // cost a reload (this is what keeps the deadline cut off the Netty event loop).
        clock.add(Duration.ofMinutes(2)); // now past the deadline
        assertThat(resolver.resolve("prev-fp")).isEmpty();
        assertThat(resolver.resolve("prev-fp")).isEmpty();

        verify(instanceService, times(1)).findByActiveOrNextOrPreviousFingerprint("prev-fp");
    }

    @Test
    void idleEntryExpiresAndReloadsOnNextResolve() {
        final var instance = activeInstance("uid-idle", "fp-idle", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-idle")).thenReturn(Optional.of(instance));

        assertThat(resolver.resolve("fp-idle")).contains("uid-idle");

        clock.add(Duration.ofMinutes(61)); // exceeds IDLE_EXPIRY without any access
        assertThat(resolver.resolve("fp-idle")).contains("uid-idle");

        verify(instanceService, times(2)).findByActiveOrNextOrPreviousFingerprint("fp-idle");
    }

    @Test
    void touchedFingerprintThatIsCachedIsReResolved() {
        final var instance = activeInstance("uid-1", "fp-1", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1")).thenReturn(Optional.of(instance));
        assertThat(resolver.resolve("fp-1")).contains("uid-1");

        // The instance is deleted: the fingerprint no longer binds. A certs-changed event touching the
        // still-cached fingerprint re-resolves it (to unbound).
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1")).thenReturn(Optional.empty());
        resolver.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-1")));

        assertThat(resolver.resolve("fp-1")).isEmpty();
        verify(instanceService, times(2)).findByActiveOrNextOrPreviousFingerprint("fp-1");
    }

    @Test
    void refreshInstallsGraceStampOnLiveEntry() {
        // Rotation scenario: the fingerprint is cached as a plain active binding, then rotation demotes it
        // to the previous slot — the certs-changed refresh must re-resolve the live entry so it picks up
        // the grace deadline, and the subsequent cut must again be reload-free.
        final var active = activeInstance("uid-rot", "fp-rot", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-rot")).thenReturn(Optional.of(active));
        assertThat(resolver.resolve("fp-rot")).contains("uid-rot");

        stubGracePeriod(GRACE_PERIOD);
        final var demoted = previousInstance("uid-rot", "fp-rot", clock.instant(),
                clock.instant().plus(Duration.ofDays(90)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-rot")).thenReturn(Optional.of(demoted));
        resolver.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-rot")));

        assertThat(resolver.resolve("fp-rot")).contains("uid-rot"); // still within grace

        clock.add(GRACE_PERIOD.plus(Duration.ofMinutes(1)));
        assertThat(resolver.resolve("fp-rot")).isEmpty();
        verify(instanceService, times(2)).findByActiveOrNextOrPreviousFingerprint("fp-rot"); // load + refresh
    }

    @Test
    void failedReResolveServesStaleUntilRefreshSucceeds() {
        final var instance = activeInstance("uid-1", "fp-1", clock.instant().plus(Duration.ofDays(365)));
        // Call sequence: initial load binds; the event-driven re-resolve hits a Mongo outage; the
        // later refreshAfterWrite re-resolve succeeds against recovered Mongo (instance revoked).
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1"))
                .thenReturn(Optional.of(instance))
                .thenThrow(new RuntimeException("mongo down"))
                .thenReturn(Optional.empty());
        assertThat(resolver.resolve("fp-1")).contains("uid-1");

        // The certs-changed event says this fingerprint's binding changed (the instance was revoked),
        // but the re-resolve fails. Serve-stale: the last authoritative state keeps being served — the
        // cut must not depend on Mongo being up at the moment of the event.
        resolver.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-1")));
        assertThat(resolver.resolve("fp-1")).contains("uid-1");

        // Mongo recovers. The failed refresh did not renew the entry's write timestamp, so once the
        // entry is older than the refreshAfterWrite interval, an access re-resolves it and the missed
        // revocation is applied — the bounded self-healing that makes serve-stale safe. (The value the
        // triggering access itself returns depends on executor timing; the inline test executor
        // completes the reload immediately, so only the post-trigger state is asserted.)
        clock.add(Duration.ofMinutes(31)); // past refreshAfterWrite (30m), within idle expiry (60m)
        resolver.resolve("fp-1"); // triggers the re-resolve
        assertThat(resolver.resolve("fp-1")).isEmpty(); // revocation applied

        verify(instanceService, times(3)).findByActiveOrNextOrPreviousFingerprint("fp-1");
    }

    @Test
    void staleEntryIsReResolvedOnAccessAfterRefreshInterval() {
        final var instance = activeInstance("uid-1", "fp-1", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1")).thenReturn(Optional.of(instance));
        assertThat(resolver.resolve("fp-1")).contains("uid-1");

        // The instance is revoked but the certs-changed event never arrives (lost cluster event).
        // refreshAfterWrite is the backstop: an access past the interval re-resolves the entry.
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1")).thenReturn(Optional.empty());
        clock.add(Duration.ofMinutes(31)); // past refreshAfterWrite (30m), within idle expiry (60m)
        resolver.resolve("fp-1"); // triggers the background re-resolve

        assertThat(resolver.resolve("fp-1")).isEmpty();
        verify(instanceService, times(2)).findByActiveOrNextOrPreviousFingerprint("fp-1");
    }

    @Test
    void touchedFingerprintThatIsNotCachedIsNotLoaded() {
        // fp-2 is not cached, so a certs-changed event must NOT proactively load it (avoids loading the
        // many fingerprints of expired instances that no longer exist).
        resolver.handleCertsChanged(new CollectorInstanceCertsChangedEvent(Set.of("fp-2")));
        verify(instanceService, never()).findByActiveOrNextOrPreviousFingerprint("fp-2");

        // It cold-loads on first resolve instead.
        final var instance = activeInstance("uid-2", "fp-2", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-2")).thenReturn(Optional.of(instance));
        assertThat(resolver.resolve("fp-2")).contains("uid-2");
        verify(instanceService, times(1)).findByActiveOrNextOrPreviousFingerprint("fp-2");
    }

    @Test
    void preWarmLoadsOnlyActiveFingerprints() {
        final var config = CollectorsConfig.createDefault("test-host");
        when(configService.getOrDefault()).thenReturn(config);
        final var instance = activeInstance("uid-pw", "active-fp", clock.instant().plus(Duration.ofDays(365)));
        when(instanceService.streamAllUnexpired(config.collectorExpirationThreshold())).thenReturn(Stream.of(instance));

        resolver.startAsync().awaitRunning();
        try {
            // The active fingerprint is prewarmed: it resolves without a document lookup.
            assertThat(resolver.resolve("active-fp")).contains("uid-pw");
            verify(instanceService, never()).findByActiveOrNextOrPreviousFingerprint(any());

            // The next fingerprint is NOT prewarmed — it cold-loads on first resolve.
            final var next = nextInstance("uid-pw", "next-fp", clock.instant().plus(Duration.ofDays(30)));
            when(instanceService.findByActiveOrNextOrPreviousFingerprint("next-fp")).thenReturn(Optional.of(next));
            assertThat(resolver.resolve("next-fp")).contains("uid-pw");
            verify(instanceService).findByActiveOrNextOrPreviousFingerprint("next-fp");
        } finally {
            resolver.stopAsync().awaitTerminated();
        }
    }

    @Test
    void certsChangedEventPostedToEventBusIsHandled() {
        final var config = CollectorsConfig.createDefault("test-host");
        when(configService.getOrDefault()).thenReturn(config);
        when(instanceService.streamAllUnexpired(any())).thenReturn(Stream.empty());

        resolver.startAsync().awaitRunning();
        try {
            final var instance = activeInstance("uid-1", "fp-1", clock.instant().plus(Duration.ofDays(365)));
            when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1")).thenReturn(Optional.of(instance));
            assertThat(resolver.resolve("fp-1")).contains("uid-1");

            when(instanceService.findByActiveOrNextOrPreviousFingerprint("fp-1")).thenReturn(Optional.empty());
            eventBus.post(new CollectorInstanceCertsChangedEvent(Set.of("fp-1")));

            assertThat(resolver.resolve("fp-1")).isEmpty();
        } finally {
            resolver.stopAsync().awaitTerminated();
        }
    }

    // ----- Fixtures -----

    private void stubGracePeriod(Duration gracePeriod) {
        when(configService.getOrDefault()).thenReturn(CollectorsConfig.createDefaultBuilder("test-host")
                .collectorCertRotationGracePeriod(gracePeriod)
                .build());
    }

    /**
     * An instance matching the fingerprint in its active slot. Only the accessors the resolver touches on
     * this code path are stubbed (Mockito strict stubbing).
     */
    private CollectorInstanceDTO activeInstance(String instanceUid, String activeFp, Instant certExpiry) {
        final var instance = mock(CollectorInstanceDTO.class);
        when(instance.instanceUid()).thenReturn(instanceUid);
        when(instance.activeCertificateFingerprint()).thenReturn(activeFp);
        when(instance.activeCertificateExpiresAt()).thenReturn(certExpiry);
        return instance;
    }

    /**
     * An instance matching the fingerprint in its previous slot ({@code rotatedAt} nullable to simulate a
     * missing rotation timestamp).
     */
    private CollectorInstanceDTO previousInstance(String instanceUid, String previousFp, Instant rotatedAt,
                                                  Instant certExpiry) {
        final var instance = mock(CollectorInstanceDTO.class);
        when(instance.instanceUid()).thenReturn(instanceUid);
        when(instance.activeCertificateFingerprint()).thenReturn("fp-other-active");
        when(instance.previousCertificateFingerprint()).thenReturn(Optional.of(previousFp));
        when(instance.certificatesRotatedAt()).thenReturn(Optional.ofNullable(rotatedAt));
        when(instance.previousCertificateExpiresAt()).thenReturn(Optional.of(certExpiry));
        return instance;
    }

    /**
     * An instance matching the fingerprint in its next slot.
     */
    private CollectorInstanceDTO nextInstance(String instanceUid, String nextFp, Instant certExpiry) {
        final var instance = mock(CollectorInstanceDTO.class);
        when(instance.instanceUid()).thenReturn(instanceUid);
        when(instance.activeCertificateFingerprint()).thenReturn("fp-other-active");
        when(instance.previousCertificateFingerprint()).thenReturn(Optional.empty());
        when(instance.nextCertificateFingerprint()).thenReturn(Optional.of(nextFp));
        when(instance.nextCertificateExpiresAt()).thenReturn(Optional.of(certExpiry));
        return instance;
    }
}
