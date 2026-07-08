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
package org.graylog.security.certutil;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.security.TrustManagerAndSocketFactoryProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Mints short-lived in-memory client certificates signed by Graylog's CA and wraps them in an
 * {@link SSLContext} ready for use with HTTPS clients that require certificate-based
 * authentication. The certificate, its private key, and all derived material live only in
 * memory for the duration of the returned context.
 *
 * <p>Requires a configured CA. Returns {@link Optional#empty()} if no CA is present.
 */
@Singleton
public class ClientCertSslContextFactoryImpl implements ClientCertSslContextFactory {

    private static final String KEY_ALIAS = "client";

    private final CaKeystore caKeystore;
    private final TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider;

    @Inject
    public ClientCertSslContextFactoryImpl(CaKeystore caKeystore,
                                           TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider) {
        this.caKeystore = caKeystore;
        this.trustManagerAndSocketFactoryProvider = trustManagerAndSocketFactoryProvider;
    }

    /**
     * Generates a fresh key pair, has it signed by Graylog's CA with the given common name and
     * lifetime, and returns an {@link SSLContext} configured with that cert for client auth and
     * the platform's configured trust manager for server verification.
     */
    @Override
    public Optional<SSLContext> buildClientCertSslContext(String commonName, Duration certificateLifetime) {
        if (!caKeystore.exists()) {
            return Optional.empty();
        }
        try {
            final CertRequest certRequest = CertRequest.selfSigned(commonName)
                    .isCA(false)
                    .validity(certificateLifetime);
            final KeyPair keyPair = CertificateGenerator.generate(certRequest);

            final char[] keystorePassword = RandomStringUtils.secure().nextAlphanumeric(96).toCharArray();
            final KeyStore csrKeystore = keyPair.toKeystore(KEY_ALIAS, keystorePassword);
            final InMemoryKeystoreInformation csrKeystoreInfo = new InMemoryKeystoreInformation(csrKeystore, keystorePassword);

            final PKCS10CertificationRequest csr = CsrGenerator.generateCSR(
                    csrKeystoreInfo, KEY_ALIAS, commonName, List.of());
            final CertificateChain certChain = caKeystore.signCertificateRequest(
                    new CertificateSigningRequest(commonName, csr), certificateLifetime);

            final KeyStore signedKeystore = KeyStore.getInstance("PKCS12");
            signedKeystore.load(null, null);
            signedKeystore.setKeyEntry(KEY_ALIAS, keyPair.privateKey(), keystorePassword, certChain.toCertificateChainArray());

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(signedKeystore, keystorePassword);
            final X509TrustManager trustManager = trustManagerAndSocketFactoryProvider.getTrustManager();
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), new TrustManager[]{trustManager}, new SecureRandom());
            return Optional.of(sslContext);
        } catch (CaKeystoreException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(f("Failed to build client-cert SSL context for CN=%s", commonName), e);
        }
    }
}
