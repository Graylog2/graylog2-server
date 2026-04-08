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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.events.CollectorCaConfigUpdated;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

/**
 * Provides a CA cache that caches {@link CertificateEntry} instances based on their expiration date.
 */
@Singleton
public class CollectorCaCache extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorCaCache.class);

    private static final String SERVER_KEY = "_static_:server";
    private static final String SIGNING_KEY = "_static_:signing";
    private static final String CA_KEY = "_static_:ca";

    private final CollectorCaService caService;
    private final CertificateService certificateService;
    private final EncryptedValueService encryptedValueService;
    private final EventBus eventBus;
    private final Cache<String, CacheEntry> cache;

    public record CacheEntry(PrivateKey privateKey, X509Certificate cert, String fingerprint) {
    }

    @Inject
    public CollectorCaCache(CollectorCaService caService,
                            CertificateService certificateService,
                            EncryptedValueService encryptedValueService,
                            EventBus eventBus,
                            Clock clock) {
        this.caService = caService;
        this.certificateService = certificateService;
        this.encryptedValueService = encryptedValueService;
        this.eventBus = eventBus;
        this.cache = Caffeine.newBuilder()
                .expireAfter(Expiry.<String, CacheEntry>creating((key, value) ->
                        Duration.between(Instant.now(clock), value.cert().getNotAfter().toInstant())))
                .initialCapacity(3)
                .build();
    }

    /**
     * Get entry by certificate Subject Key Identifier.
     *
     * @param ski the cert Subject Key Identifier value
     * @return the cache entry or an empty optional
     */
    public Optional<CacheEntry> getBySubjectKeyIdentifier(String ski) {
        requireNonBlank(ski, "Subject Key Identifier can't be blank");

        return Optional.ofNullable(cache.get(ski, key -> getCacheEntry(
                () -> certificateService.findBySubjectKeyIdentifier(ski).orElse(null)
        ).orElse(null)));
    }

    /**
     * Get the server entry.
     *
     * @return the server entry
     */
    public CacheEntry getServer() {
        return cache.get(SERVER_KEY, key -> getCacheEntry(caService::getOtlpServerCert).orElseThrow(() -> new IllegalStateException("Server certificate not found")));
    }

    /**
     * Get the signing entry.
     *
     * @return the signing entry
     */
    public CacheEntry getSigning() {
        return cache.get(SIGNING_KEY, key -> getCacheEntry(caService::getSigningCert).orElseThrow(() -> new IllegalStateException("Signing certificate not found")));
    }

    /**
     * Get the CA entry.
     *
     * @return the CA entry
     */
    public CacheEntry getCa() {
        return cache.get(CA_KEY, key -> getCacheEntry(caService::getCaCert).orElseThrow(() -> new IllegalStateException("CA certificate not found")));
    }

    private Optional<CacheEntry> getCacheEntry(Supplier<CertificateEntry> certSupplier) {
        try {
            final var certEntry = certSupplier.get();
            if (certEntry == null) {
                return Optional.empty();
            }
            final var cert = PemUtils.parseCertificate(certEntry.certificate());
            final var privateKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(certEntry.privateKey()));
            LOG.debug("Loaded cert <{}>", certEntry.fingerprint());
            return Optional.of(new CacheEntry(privateKey, cert, certEntry.fingerprint()));
        } catch (Exception e) {
            LOG.error("Couldn't load certificate", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
    }

    @Subscribe
    @VisibleForTesting
    void handleCollectorsConfigEvent(CollectorCaConfigUpdated ignored) {
        cache.invalidateAll();
    }
}
