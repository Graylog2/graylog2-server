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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.assistedinject.Assisted;
import org.bouncycastle.est.jcajce.JsseDefaultHostnameAuthorizer;

import javax.inject.Inject;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

public class DefaultX509TrustManager extends X509ExtendedTrustManager {
    private final String host;
    private final X509TrustManager defaultTrustManager;
    private final JsseDefaultHostnameAuthorizer authorizer;

    @Inject
    public DefaultX509TrustManager(@Assisted String host) throws NoSuchAlgorithmException, KeyStoreException {
        this(host, null);
    }

    @VisibleForTesting
    public DefaultX509TrustManager(String host, KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
        super();
        this.authorizer = new JsseDefaultHostnameAuthorizer(Collections.emptySet());

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        this.defaultTrustManager = Arrays.stream(tmf.getTrustManagers())
                .filter(trustManager -> trustManager instanceof X509TrustManager)
                .map(trustManager -> (X509TrustManager)trustManager)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to initialize default X509 trust manager."));

        this.host = host;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.defaultTrustManager.getAcceptedIssuers();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        this.defaultTrustManager.checkServerTrusted(x509Certificates, s);
        validateHostnames(x509Certificates, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        checkServerTrusted(x509Certificates, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        checkServerTrusted(x509Certificates, s);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        checkClientTrusted(x509Certificates, s);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        checkClientTrusted(x509Certificates, s);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        this.defaultTrustManager.checkClientTrusted(x509Certificates, s);
        validateHostnames(x509Certificates, s);
    }

    private void validateHostnames(X509Certificate[] x509Certificates, String s) throws CertificateException {
        Arrays.stream(x509Certificates)
                .filter(this::certificateMatchesHostname)
                .findFirst()
                .orElseThrow(() -> new CertificateException("Presented certificate does not match configured hostname!"));
    }

    private boolean certificateMatchesHostname(X509Certificate x509Certificate) {
        try {
            return this.authorizer.verify(this.host, x509Certificate);
        } catch (IOException e) {
            return false;
        }
    }
}
