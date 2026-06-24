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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.events.CollectorInstanceCertsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Caches the mapping from a collector client-certificate fingerprint to the instance UID it belongs to, so
 * the ingest mTLS path can resolve a presented certificate to an active collector instance without a
 * MongoDB lookup on every request.
 * <p>
 * Misses load from {@link CollectorInstanceService#findByActiveOrNextFingerprint(String)} (caching both
 * hits and "no active instance" results); idle entries are evicted; the cache is kept consistent by
 * {@link CollectorInstanceCertsChangedEvent}s and prewarmed with the currently-active instances at startup.
 * Asynchronous work (refreshes, prewarm) runs on the {@link CollectorCertVerificationExecutor}.
 */
@Singleton
public class CollectorFingerprintCache extends AbstractIdleService {

    public static final long MAX_SIZE = 1_000_000L; // safety net
    private static final Logger LOG = LoggerFactory.getLogger(CollectorFingerprintCache.class);

    private final CollectorsConfigService configService;
    private final CollectorInstanceService instanceService;
    private final Executor executor;
    private final EventBus eventBus;

    private final LoadingCache<String, Optional<String>> cache;

    @Inject
    public CollectorFingerprintCache(CollectorsConfigService configService,
                                     CollectorInstanceService instanceService,
                                     EventBus eventBus,
                                     @CollectorCertVerificationExecutor Executor executor) {
        this.configService = configService;
        this.instanceService = instanceService;
        this.executor = executor;
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterAccess(Duration.ofMinutes(30))
                .executor(executor) // event-driven refresh will run on this executor
                .build(fingerprint ->
                        instanceService.findByActiveOrNextFingerprint(fingerprint)
                                .map(CollectorInstanceDTO::instanceUid));
        this.eventBus = eventBus;
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
     * Returns an empty optional if no active instance has the fingerprint, or if the lookup fails (fail closed).
     */
    public Optional<String> lookup(String fingerprint) {
        try {
            return cache.get(fingerprint);
        } catch (Exception e) {
            LOG.error("Error looking up collector instance for fingerprint {}.", fingerprint, e);
        }
        return Optional.empty();
    }

    /**
     * Keeps the cache consistent with certificate changes: invalidates removed fingerprints (re-resolved on
     * next access) and refreshes added ones into the cache.
     */
    @Subscribe
    public void handleCertsChanged(CollectorInstanceCertsChangedEvent event) {
        cache.invalidateAll(event.removedFingerprints());
        cache.refreshAll(event.addedFingerprints());
    }

    /**
     * Loads the currently-active instances' fingerprints into the cache at startup (bounded by
     * {@link #MAX_SIZE}), so a fleet reconnecting after a restart hits a warm cache instead of loading on
     * each handshake. Best-effort: a failure leaves the cache to populate lazily.
     */
    private void preWarm() {
        try {
            final var expirationThreshold = configService.getOrDefault().collectorExpirationThreshold();
            try (var stream = instanceService.streamAllUnexpired(expirationThreshold).limit(MAX_SIZE)) {
                stream.forEach(instance -> {
                    cache.put(instance.activeCertificateFingerprint(), Optional.of(instance.instanceUid()));
                    instance.nextCertificateFingerprint().ifPresent(fp ->
                            cache.put(fp, Optional.of(instance.instanceUid())));
                });
            }
        } catch (Exception e) {
            LOG.warn("Failed pre-warming collector fingerprint cache.", e);
        }
    }

}
