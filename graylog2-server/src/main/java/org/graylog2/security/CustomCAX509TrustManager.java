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
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
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
    private final List<X509TrustManager> trustManagers = new ArrayList<>();
    private final DefaultX509TrustManager defaultX509TrustManager;

    @AssistedInject
    public CustomCAX509TrustManager(@Assisted String host, CaService caService) throws NoSuchAlgorithmException, KeyStoreException {
        this(host != null ? List.of(host) : List.of(), caService);
    }

    /**
     * Create a X509TrustManager that verifies the certificate chain and checks whether the cert matches
     * one of the given hosts in the list.
     * <p>
     * <b>Note: ANY matching host from the list is accepted. </b> <br>
     *    E.g.: Given a host list [A,B], the server B is allowed to offer a certificate issued to A
     * @param hosts     The hosts to check the certificate subject against
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    @AssistedInject
    public CustomCAX509TrustManager(@Assisted List<String> hosts, CaService caService) throws NoSuchAlgorithmException, KeyStoreException {
        defaultX509TrustManager = new DefaultX509TrustManager(hosts);
        trustManagers.add(defaultX509TrustManager);
        try {
            caService.loadKeyStore().ifPresent(keystore -> trustManagers.add(getTrustManager(keystore)));
        } catch (KeyStoreException | KeyStoreStorageException k) {
            LOG.error("Could not add Graylog CA to TrustManagers: {}", k.getMessage(), k);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType);
                return;
            } catch (CertificateException e) {}
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain.");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException e) {}
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain.");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        final var certificates = new ArrayList<X509Certificate>();
        trustManagers.forEach(tm -> certificates.addAll(Arrays.asList(tm.getAcceptedIssuers())));
        return certificates.toArray(new X509Certificate[0]);
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
