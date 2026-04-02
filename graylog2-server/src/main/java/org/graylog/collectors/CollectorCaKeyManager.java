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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.events.CollectorCaConfigUpdated;
import org.graylog.security.pki.PemUtils;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Singleton
public class CollectorCaKeyManager extends AbstractIdleService implements X509KeyManager {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorCaKeyManager.class);

    private final CollectorCaService caService;
    private final EncryptedValueService encryptedValueService;
    private final EventBus eventBus;
    private final LoadingCache<Integer, CacheEntry> cache;

    private record CacheEntry(PrivateKey privateKey, X509Certificate serverCert, X509Certificate signingCert) {
    }

    @Inject
    public CollectorCaKeyManager(CollectorCaService caService,
                                 EncryptedValueService encryptedValueService,
                                 EventBus eventBus) {
        this.caService = caService;
        this.encryptedValueService = encryptedValueService;
        this.eventBus = eventBus;
        this.cache = Caffeine.newBuilder()
                .expireAfter(Expiry.<Integer, CacheEntry>creating((key, value) -> Duration.ofSeconds(1)))
                .maximumSize(1)
                .initialCapacity(1)
                .build(this::loadCacheKey);
    }

    private CacheEntry loadCacheKey(Integer key) {
        LOG.info("Loading key {}", key);
        try {
            final var signingCertEntry = caService.getOtlpServerCert();
            final var serverCertEntry = caService.getOtlpServerCert();
            final var signingCert = PemUtils.parseCertificate(signingCertEntry.certificate());
            final var serverCert = PemUtils.parseCertificate(serverCertEntry.certificate());
            final var privateKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(serverCertEntry.privateKey()));
            return new CacheEntry(privateKey, serverCert, signingCert);
        } catch (Exception e) {
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
    @SuppressWarnings("unused")
    public void handleCollectorsConfigEvent(CollectorCaConfigUpdated event) {
        cache.invalidateAll();
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return keyType;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        LOG.warn("getCertificateChain alias={}", alias);
        if ("EdDSA".equals(alias)) {
            final var entry = cache.get(0);
            return new X509Certificate[]{entry.serverCert(), entry.signingCert()};
        }
        return null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        LOG.warn("getPrivateKey alias={}", alias);
        if ("EdDSA".equals(alias)) {
            return cache.get(0).privateKey();
        }
        return null;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return null;
    }
}
