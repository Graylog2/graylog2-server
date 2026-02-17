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
import org.graylog2.shared.utilities.StringUtils;

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

public class TrustManagerAggregator implements X509TrustManager {

    private final List<X509TrustManager> trustManagers;

    public TrustManagerAggregator(List<X509TrustManager> trustManagers) {
        this.trustManagers = trustManagers;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        List<CertificateException> failures = new ArrayList<>();

        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
                failures.add(e);
            }
        }

        throw createDetailedException("client", chain, authType, failures);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        List<CertificateException> failures = new ArrayList<>();

        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
                failures.add(e);
            }
        }

        throw createDetailedException("server", chain, authType, failures);
    }

    private CertificateException createDetailedException(
            String trustType,
            X509Certificate[] chain,
            String authType,
            List<CertificateException> failures) {

        StringBuilder message = new StringBuilder();
        message.append(StringUtils.f("Certificate chain validation failed for %s trust. ", trustType));
        message.append(StringUtils.f("Tried %d trust manager(s), all rejected the certificate. ", failures.size()));

        if (chain != null && chain.length > 0) {
            X509Certificate cert = chain[0];
            message.append(StringUtils.f(
                    "Certificate details - Subject: '%s', Issuer: '%s', Valid: %s to %s. ",
                    cert.getSubjectX500Principal().getName(),
                    cert.getIssuerX500Principal().getName(),
                    cert.getNotBefore(),
                    cert.getNotAfter()
            ));
        }

        message.append(StringUtils.f("Auth type: %s. ", authType));
        message.append(StringUtils.f("Chain length: %d. ", chain != null ? chain.length : 0));

        CertificateException exception = new CertificateException(message.toString());

        // Add all individual failures as suppressed exceptions for detailed debugging
        for (int i = 0; i < failures.size(); i++) {
            CertificateException failure = failures.get(i);
            CertificateException wrappedException = new CertificateException(
                    "Trust manager #" + (i + 1) + " rejection: " + failure.getMessage(),
                    failure
            );
            exception.addSuppressed(wrappedException);
        }

        return exception;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        final var certificates = new ArrayList<X509Certificate>();
        trustManagers.forEach(tm -> certificates.addAll(Arrays.asList(tm.getAcceptedIssuers())));
        return certificates.toArray(new X509Certificate[0]);
    }

    public static X509TrustManager trustManagerFromKeystore(KeyStore keystore) {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keystore);
            return Iterables.getFirst(Iterables.filter(Arrays.asList(factory.getTrustManagers()), X509TrustManager.class), null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
