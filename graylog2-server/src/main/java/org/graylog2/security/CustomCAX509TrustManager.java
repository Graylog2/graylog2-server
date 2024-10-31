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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.graylog.security.certutil.CaTruststore;
import org.graylog.security.certutil.CaTruststoreException;
import org.graylog.security.certutil.CertificateAuthorityChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

public class CustomCAX509TrustManager implements X509TrustManager {
    private static final Logger LOG = LoggerFactory.getLogger(CustomCAX509TrustManager.class);
    private final CaTruststore caTruststore;
    private volatile X509TrustManager delegate;

    @Inject
    public CustomCAX509TrustManager(CaTruststore caTruststore, EventBus serverEventBus) {
        this.caTruststore = caTruststore;
        serverEventBus.register(this);
        this.refresh();
    }

    @Subscribe
    public void handleCertificateAuthorityChange(CertificateAuthorityChangedEvent event) {
        LOG.info("CA changed, refreshing trust manager");
        refresh();
    }

    public synchronized void refresh() {
        try {
            List<X509TrustManager> trustManagers = new LinkedList<>();
            trustManagers.add(getDefaultTrustManager());
            caTruststore.getTrustStore()
                    .map(TrustManagerAggregator::trustManagerFromKeystore)
                    .ifPresent(trustManagers::add);
            this.delegate = new TrustManagerAggregator(trustManagers);
        } catch (CaTruststoreException | KeyStoreException | NoSuchAlgorithmException k) {
            LOG.error("Could not add Graylog CA to TrustManagers: {}", k.getMessage(), k);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    private X509TrustManager getDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        return TrustManagerAggregator.trustManagerFromKeystore(null);
    }
}
