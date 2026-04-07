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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Custom trust manager that looks up the trust chain via authority key identifiers. This allows efficient certificate
 * lookup with multiple active signing certs. (e.g., cert renewal)
 */
@Singleton
public class CollectorCaTrustManager implements X509TrustManager {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorCaTrustManager.class);

    private final CollectorCaCache caCache;
    private final Clock clock;

    @Inject
    public CollectorCaTrustManager(CollectorCaCache caCache, Clock clock) {
        this.caCache = caCache;
        this.clock = clock;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        if (certs == null || certs.length == 0) {
            throw new CertificateException("No client certificates provided");
        }

        final var clientCert = certs[0];

        // Extract the AKI from the client cert to find the issuing certificate by its SKI
        final var aki = PemUtils.extractAuthorityKeyIdentifier(clientCert)
                .orElseThrow(() -> new CertificateException("Client certificate has no Authority Key Identifier"));

        final var issuerEntry = caCache.getBySubjectKeyIdentifier(aki)
                .orElseThrow(() -> new CertificateException("No known issuer for Authority Key Identifier: " + aki));

        try {
            clientCert.verify(issuerEntry.cert().getPublicKey());
            clientCert.checkValidity(Date.from(Instant.now(clock)));
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Client certificate verification failed", e);
        }

        LOG.debug("Client certificate trusted: subject=<{}>, issuer=<{}>",
                clientCert.getSubjectX500Principal(), clientCert.getIssuerX500Principal());
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        throw new UnsupportedOperationException("#checkServerTrusted() not implemented");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // Return an empty array so the TLS CertificateRequest places no restriction on the
        // client's issuer. The actual trust decision happens in checkClientTrusted via SKI lookup,
        // which supports multiple signing certs (e.g. during cert renewal).
        return new X509Certificate[0];
    }
}
