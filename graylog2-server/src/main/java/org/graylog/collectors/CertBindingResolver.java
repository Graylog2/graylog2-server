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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.events.CollectorInstanceCertsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resolves a client-certificate fingerprint to the collector instance it currently binds to — the single
 * authority on ingest-path certificate validity. A fingerprint binds while it occupies one of the
 * instance's certificate slots (active, next, or previous) and the binding is within its validity: the
 * certificate's own expiry, capped for a superseded (previous-slot) certificate by the rotation grace
 * period ({@link CollectorsConfig#collectorCertRotationGracePeriod()}). Since TLS never re-validates an
 * established connection, this per-request resolution is the sole post-handshake cut for rotated, revoked,
 * and expired certificates.
 * <p>
 * Resolved bindings are cached internally so the ingest mTLS path avoids a MongoDB lookup per request.
 * Entries expire after a constant idle window ({@link #IDLE_EXPIRY}, sliding on access) — deliberately
 * longer than the ingest transport's forced idle connection timeout, so an entry warmed at the TLS
 * handshake always outlives its connection and per-request resolutions never load on the Netty event
 * loop. Validity is enforced on every read, so a cut never requires a reload. The cache is prewarmed at
 * startup with active fingerprints and kept consistent by {@link CollectorInstanceCertsChangedEvent}s,
 * which refresh touched entries in place.
 * <p>
 * Refreshes are <em>serve-stale-while-revalidating</em>: a failed reload retains the current value,
 * and {@code refreshAfterWrite} re-resolves every entry that is still being accessed once its value
 * is older than half the idle window.
 */
@Singleton
public class CertBindingResolver extends AbstractIdleService {

    public static final long MAX_SIZE = 1_000_000L; // safety net
    // Expiry must stay well above the ingest transport's forced idle connection timeout, so that an entry warmed
    // at the TLS handshake always outlives its connection and per-request lookups stay cache hits.
    static final Duration IDLE_EXPIRY = Duration.ofHours(1);
    private static final Logger LOG = LoggerFactory.getLogger(CertBindingResolver.class);

    private final CollectorsConfigService configService;
    private final CollectorInstanceService instanceService;
    private final Executor executor;
    private final EventBus eventBus;
    private final Clock clock;

    private final LoadingCache<String, Optional<CertBinding>> cache;
    private volatile List<CollectorInstanceCertsChangedEvent> bufferedEventsBeforeCachePrewarming;

    @Inject
    public CertBindingResolver(CollectorsConfigService configService,
                               CollectorInstanceService instanceService,
                               EventBus eventBus,
                               Clock clock,
                               @CollectorCertCacheRefreshExecutor Executor executor) {
        this(configService, instanceService, eventBus, clock, null, executor);
    }

    @VisibleForTesting
    CertBindingResolver(CollectorsConfigService configService,
                        CollectorInstanceService instanceService,
                        EventBus eventBus,
                        Clock clock,
                        @Nullable Ticker cacheTicker,
                        @CollectorCertCacheRefreshExecutor Executor executor) {
        this.configService = configService;
        this.instanceService = instanceService;
        this.executor = executor;
        this.eventBus = eventBus;
        this.clock = clock;
        final var cacheBuilder = Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .executor(executor) // refresh runs on this executor
                .expireAfterAccess(IDLE_EXPIRY)
                .refreshAfterWrite(IDLE_EXPIRY.dividedBy(2)); // self-healing bound for failed/missed refreshes
        if (cacheTicker != null) {
            cacheBuilder.ticker(cacheTicker);
        }
        this.cache = cacheBuilder.build(this::loadBinding);
        this.bufferedEventsBeforeCachePrewarming = new ArrayList<>();
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);
        executor.execute(this::preWarm);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
    }

    /**
     * Resolves a certificate fingerprint to the instance UID it currently binds to. Returns an empty
     * optional if the fingerprint is unknown, its binding is no longer valid (superseded certificate past
     * the rotation grace period, or expired certificate), or resolution fails.
     */
    public Optional<String> resolve(String fingerprint) {
        try {
            return cache.get(fingerprint)
                    .filter(binding -> binding.isValidAt(clock.instant()))
                    .map(CertBinding::instanceUid);
        } catch (Exception e) {
            LOG.error("Error resolving collector instance for fingerprint {}.", fingerprint, e);
        }
        return Optional.empty();
    }

    /**
     * Maps a fingerprint to its binding from the instance's certificate slots. A pure mapping — no clock
     * is consulted: every binding carries the instant it stops being honored
     * ({@link CertBinding#validUntil()}), and {@link #resolve(String)} evaluates validity against it.
     * Slot fields that should exist but don't (corrupt document) yield an already-expired binding rather
     * than an error.
     */
    private Optional<CertBinding> loadBinding(String fingerprint) {
        final var binding = instanceService.findByActiveOrNextOrPreviousFingerprint(fingerprint)
                .map(instance -> {
                    if (fingerprint.equals(instance.activeCertificateFingerprint())) {
                        return loadedBinding(fingerprint, instance.instanceUid(), "active",
                                instance.activeCertificateExpiresAt());
                    }
                    if (instance.previousCertificateFingerprint().filter(fingerprint::equals).isPresent()) {
                        final var gracePeriod = configService.getOrDefault().collectorCertRotationGracePeriod();
                        final var graceDeadline = instance.certificatesRotatedAt()
                                .map(rotatedAt -> rotatedAt.plus(gracePeriod))
                                .orElse(Instant.MIN);
                        final var certExpiry = instance.previousCertificateExpiresAt().orElse(Instant.MIN);
                        return loadedBinding(fingerprint, instance.instanceUid(), "previous",
                                graceDeadline.isBefore(certExpiry) ? graceDeadline : certExpiry);
                    }
                    if (instance.nextCertificateFingerprint().filter(fingerprint::equals).isPresent()) {
                        final var certExpiry = instance.nextCertificateExpiresAt().orElse(Instant.MIN);
                        return loadedBinding(fingerprint, instance.instanceUid(), "next", certExpiry);
                    }
                    return null;
                });
        if (binding.isEmpty()) {
            LOG.debug("No instance binds fingerprint {}.", fingerprint);
        }
        return binding;
    }

    private static CertBinding loadedBinding(String fingerprint, String instanceUid, String slot, Instant validUntil) {
        LOG.debug("Loaded binding for fingerprint {}: instance {}, slot {}, valid until {}.",
                fingerprint, instanceUid, slot, validUntil);
        return new CertBinding(instanceUid, validUntil);
    }

    /**
     * Entry point for certificate-change events. Until the startup prewarm has completed, events are
     * buffered and replayed afterwards: processing them right away would race the prewarm inserts — a
     * refresh-if-present for a not-yet-inserted fingerprint is a no-op, and prewarm would then insert its
     * stale snapshot value over the event's newer truth. Replaying after prewarm is always safe because a
     * refresh reloads the current database state, so ordering and duplication don't matter.
     */
    @Subscribe
    public void handleCertsChanged(CollectorInstanceCertsChangedEvent event) {
        if (bufferedEventsBeforeCachePrewarming != null) {
            synchronized (this) {
                if (bufferedEventsBeforeCachePrewarming != null) {
                    bufferedEventsBeforeCachePrewarming.add(event);
                    return;
                }
            }
        }
        doHandleCertsChanged(event);
    }

    /**
     * Keeps the cache consistent with certificate changes by re-resolving each touched fingerprint that is
     * currently cached. Absent fingerprints are left alone: they hold no stale binding and will resolve
     * correctly on their next lookup, so proactively loading them would only add pointless work (e.g. for
     * the many fingerprints of expired instances that no longer exist).
     * <p>
     * A failed re-resolve retains the current value (serve-stale): the entry's write timestamp is not
     * renewed, so {@code refreshAfterWrite} keeps re-attempting it on later accesses until a reload
     * succeeds — see the class javadoc for the staleness bound this provides.
     */
    private void doHandleCertsChanged(CollectorInstanceCertsChangedEvent event) {
        int refreshed = 0;
        for (final var fingerprint : event.fingerprints()) {
            if (cache.asMap().containsKey(fingerprint)) {
                cache.refresh(fingerprint);
                refreshed++;
            }
        }
        LOG.debug("Certificates changed: refreshing {} cached of {} touched fingerprint(s).",
                refreshed, event.fingerprints().size());
    }

    /**
     * Loads each unexpired instance's active certificate fingerprint into the cache at startup (bounded by
     * {@link #MAX_SIZE}), so a fleet reconnecting after a restart hits a warm cache instead of cold-loading
     * on each handshake. Only the active fingerprint is prewarmed: it is what essentially every ingest
     * connection presents in steady state (the hot set). The {@code next} and {@code previous} fingerprints
     * are held only by the few instances mid-renewal or within the post-activation grace window, so they
     * cold-load off the event loop on first use without risking a stampede. Best-effort: a failure leaves
     * the cache to populate lazily. Success or failure, the event gate is opened afterwards (see
     * {@link #handleCertsChanged}) — buffered events are replayed, and later events are processed directly.
     */
    private void preWarm() {
        try {
            final var expirationThreshold = configService.getOrDefault().collectorExpirationThreshold();
            final var prewarmed = new AtomicLong();
            try (var stream = instanceService.streamAllUnexpired(expirationThreshold).limit(MAX_SIZE)) {
                stream.forEach(instance -> {
                    cache.put(instance.activeCertificateFingerprint(), Optional.of(
                            new CertBinding(instance.instanceUid(), instance.activeCertificateExpiresAt())));
                    prewarmed.incrementAndGet();
                });
            }
            LOG.debug("Prewarmed {} active collector fingerprint(s).", prewarmed.get());
        } catch (Exception e) {
            LOG.warn("Failed pre-warming collector fingerprint cache.", e);
        } finally {
            final List<CollectorInstanceCertsChangedEvent> bufferedEvents;
            synchronized (this) {
                bufferedEvents = bufferedEventsBeforeCachePrewarming;
                bufferedEventsBeforeCachePrewarming = null;
            }
            bufferedEvents.forEach(this::doHandleCertsChanged);
        }
    }

    /**
     * A certificate fingerprint's binding to a collector instance: the instance the certificate binds to
     * and the instant the binding stops being honored on the ingest path — the certificate's own expiry,
     * capped for a superseded (previous-slot) certificate by the rotation grace period. The binding is a
     * pure fact about the instance's certificate slots; whether it is valid <em>now</em> is evaluated by
     * {@link #resolve(String)} via {@link #isValidAt(Instant)}, so a loaded binding may already be
     * expired.
     */
    private record CertBinding(String instanceUid, Instant validUntil) {
        private CertBinding {
            Objects.requireNonNull(instanceUid, "instanceUid must not be null");
            Objects.requireNonNull(validUntil, "validUntil must not be null");
        }

        private boolean isValidAt(Instant instant) {
            return instant.isBefore(validUntil());
        }
    }
}
