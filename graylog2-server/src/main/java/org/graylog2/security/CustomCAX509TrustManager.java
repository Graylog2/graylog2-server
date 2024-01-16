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
package org.graylog2.security;

import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.CertificateAuthorityChangedEvent;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomCAX509TrustManager implements X509TrustManager {
    private static final Logger LOG = LoggerFactory.getLogger(CustomCAX509TrustManager.class);
    private final CaService caService;
    private List<X509TrustManager> trustManagers = new ArrayList<>();

    @Inject
    public CustomCAX509TrustManager(CaService caService, EventBus serverEventBus) {
        this.caService = caService;
        serverEventBus.register(this);
        this.refresh();
    }

    @Subscribe
    public void handleCertificateAuthorityChange(CertificateAuthorityChangedEvent event) {
        LOG.info("CA changed, refreshing trust manager");
        refresh();
    }

    public void refresh() {
        try {
            trustManagers = new ArrayList<>();
            trustManagers.add(getDefaultTrustManager());
            caService.loadKeyStore().ifPresent(keystore -> trustManagers.add(getTrustManager(keystore)));
        } catch (KeyStoreException | KeyStoreStorageException | NoSuchAlgorithmException k) {
            LOG.error("Could not add Graylog CA to TrustManagers: {}", k.getMessage(), k);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
            }
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain.");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
            }
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain.");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        final var certificates = new ArrayList<X509Certificate>();
        trustManagers.forEach(tm -> certificates.addAll(Arrays.asList(tm.getAcceptedIssuers())));
        return certificates.toArray(new X509Certificate[0]);
    }

    private X509TrustManager getDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        return getTrustManager(null);
    }

    private X509TrustManager getTrustManager(KeyStore keystore) {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keystore);
            return Iterables.getFirst(Iterables.filter(Arrays.asList(factory.getTrustManagers()), X509TrustManager.class), null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            LOG.error("Could not create TrustManager: {}", e.getMessage(), e);
        }
        return null;
    }
}
