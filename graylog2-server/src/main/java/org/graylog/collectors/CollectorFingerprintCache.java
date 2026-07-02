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
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.events.CollectorInstanceCertsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Caches the mapping from a collector client-certificate fingerprint to the instance UID it binds to, so
 * the ingest mTLS path can resolve a presented certificate without a MongoDB lookup on every request.
 * <p>
 * This is purely an optimization over {@link CollectorInstanceService#resolveCertBinding(String)}, which is the
 * authoritative binding decision (including the renewal grace window for superseded certificates). Each
 * entry is cached for exactly as long as that decision is valid: an active/next binding uses an idle
 * sliding window, while a superseded (previous-slot) binding's entry expires at its grace deadline, so it
 * stops resolving even for a continuously-used connection. The cache is prewarmed at startup with active
 * fingerprints and kept consistent by {@link CollectorInstanceCertsChangedEvent}s. Asynchronous work
 * (refreshes, prewarm) runs on the {@link CollectorCertVerificationExecutor}.
 */
@Singleton
public class CollectorFingerprintCache extends AbstractIdleService {

    public static final long MAX_SIZE = 1_000_000L; // safety net
    private static final Duration IDLE_EXPIRY = Duration.ofMinutes(30); // TODO: make configurable
    private static final Logger LOG = LoggerFactory.getLogger(CollectorFingerprintCache.class);

    private final CollectorsConfigService configService;
    private final CollectorInstanceService instanceService;
    private final Executor executor;
    private final EventBus eventBus;
    private final Clock clock;

    private final LoadingCache<String, Optional<CertBinding>> cache;

    @Inject
    public CollectorFingerprintCache(CollectorsConfigService configService,
                                     CollectorInstanceService instanceService,
                                     EventBus eventBus,
                                     Clock clock,
                                     @CollectorCertVerificationExecutor Executor executor) {
        this.configService = configService;
        this.instanceService = instanceService;
        this.executor = executor;
        this.eventBus = eventBus;
        this.clock = clock;
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .executor(executor) // event-driven refresh runs on this executor
                .ticker(clockTicker()) // drive Caffeine's expiry off the same Clock as the binding deadlines
                .expireAfter(Expiry.<String, Optional<CertBinding>>accessing((fingerprint, binding) ->
                        binding.flatMap(CertBinding::validUntil)
                                .map(deadline -> Duration.between(this.clock.instant(), deadline))
                                .map(remaining -> remaining.isNegative() ? Duration.ZERO : remaining)
                                .orElse(IDLE_EXPIRY)))
                .build(instanceService::resolveCertBinding);
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
     * Resolves a certificate fingerprint to its collector instance UID, loading and caching on a miss.
     * Returns an empty optional if the fingerprint does not bind to an instance (unknown, or a superseded
     * certificate past its grace window), or if the lookup fails.
     */
    public Optional<String> lookup(String fingerprint) {
        try {
            return cache.get(fingerprint).map(CertBinding::instanceUid);
        } catch (Exception e) {
            LOG.error("Error looking up collector instance for fingerprint {}.", fingerprint, e);
        }
        return Optional.empty();
    }

    /**
     * Keeps the cache consistent with certificate changes by re-resolving each touched fingerprint that is
     * currently cached. Absent fingerprints are left alone: they hold no stale binding and will resolve
     * correctly on their next lookup, so proactively loading them would only add pointless work (e.g. for
     * the many fingerprints of expired instances that no longer exist).
     */
    @Subscribe
    public void handleCertsChanged(CollectorInstanceCertsChangedEvent event) {
        event.fingerprints().forEach(fingerprint -> {
            if (cache.asMap().containsKey(fingerprint)) {
                cache.refresh(fingerprint);
            }
        });
    }

    private Ticker clockTicker() {
        return () -> clock.millis() * 1_000_000L;
    }

    /**
     * Loads each unexpired instance's active certificate fingerprint into the cache at startup (bounded by
     * {@link #MAX_SIZE}), so a fleet reconnecting after a restart hits a warm cache instead of cold-loading
     * on each handshake. Only the active fingerprint is prewarmed: it is what essentially every ingest
     * connection presents in steady state (the hot set). The {@code next} and {@code previous} fingerprints
     * are held only by the few instances mid-renewal or within the post-activation grace window, so they
     * cold-load off the event loop on first use without risking a stampede. Best-effort: a failure leaves
     * the cache to populate lazily.
     */
    private void preWarm() {
        try {
            final var expirationThreshold = configService.getOrDefault().collectorExpirationThreshold();
            try (var stream = instanceService.streamAllUnexpired(expirationThreshold).limit(MAX_SIZE)) {
                stream.forEach(instance ->
                        cache.put(instance.activeCertificateFingerprint(),
                                Optional.of(CertBinding.bound(instance.instanceUid()))));
            }
        } catch (Exception e) {
            LOG.warn("Failed pre-warming collector fingerprint cache.", e);
        }
    }
}
