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
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Custom trust manager that looks up the trust chain via authority key identifiers. This allows efficient certificate
 * lookup with multiple active signing certs. (e.g., cert renewal)
 * <p>
 * Extends {@link X509ExtendedTrustManager} rather than implementing {@link javax.net.ssl.X509TrustManager} because
 * Netty uses {@link javax.net.ssl.SSLEngine}-based handshakes. The JDK wraps a plain {@code X509TrustManager} in
 * an adapter that adds endpoint identification checks; extending the "Extended" variant avoids that wrapper.
 */
@Singleton
public class CollectorCaTrustManager extends X509ExtendedTrustManager {
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

        verifyIssuerIsCa(issuerEntry.cert());
        verifySignatureAndValidity(clientCert, issuerEntry.cert());
        verifyEndEntityCert(clientCert);
        verifyClientAuthEku(clientCert);

        LOG.debug("Client certificate trusted: subject=<{}>, issuer=<{}>",
                clientCert.getSubjectX500Principal(), clientCert.getIssuerX500Principal());
    }

    private void verifyIssuerIsCa(X509Certificate issuerCert) throws CertificateException {
        if (issuerCert.getBasicConstraints() < 0) {
            throw new CertificateException("Issuer certificate is not a CA");
        }

        final boolean[] keyUsage = issuerCert.getKeyUsage();
        // keyUsage[5] is keyCertSign
        if (keyUsage == null || !keyUsage[5]) {
            throw new CertificateException("Issuer certificate does not have keyCertSign key usage");
        }

        // Collector certs are capped to the signing cert's remaining lifetime in CertificateBuilder#signCsr,
        // so an expired issuer means the collector cert should also have expired.
        issuerCert.checkValidity(Date.from(Instant.now(clock)));

        // Verify the issuer chains back to the collectors root CA
        try {
            final var rootCa = caCache.getCa();
            issuerCert.verify(rootCa.cert().getPublicKey());
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Issuer certificate is not signed by the collectors root CA", e);
        }
    }

    private void verifySignatureAndValidity(X509Certificate clientCert, X509Certificate issuerCert) throws CertificateException {
        try {
            clientCert.verify(issuerCert.getPublicKey());
            clientCert.checkValidity(Date.from(Instant.now(clock)));
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Client certificate verification failed", e);
        }
    }

    private void verifyEndEntityCert(X509Certificate clientCert) throws CertificateException {
        final var basicConstraints = clientCert.getBasicConstraints();
        if (basicConstraints >= 0) {
            throw new CertificateException("Client certificate must be an end-entity certificate, not a CA");
        }
    }

    private void verifyClientAuthEku(X509Certificate clientCert) throws CertificateException {
        try {
            final var eku = clientCert.getExtendedKeyUsage();
            if (eku == null || !eku.contains(KeyPurposeId.id_kp_clientAuth.getId())) {
                throw new CertificateException("Client certificate does not have the clientAuth extended key usage");
            }
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Failed to check extended key usage", e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType, Socket socket) throws CertificateException {
        checkClientTrusted(certs, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType, SSLEngine engine) throws CertificateException {
        checkClientTrusted(certs, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        throw new UnsupportedOperationException("#checkServerTrusted() not implemented");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType, Socket socket) throws CertificateException {
        throw new UnsupportedOperationException("#checkServerTrusted() not implemented");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType, SSLEngine engine) throws CertificateException {
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
