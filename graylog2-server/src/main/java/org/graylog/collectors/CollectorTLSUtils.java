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

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import jakarta.inject.Inject;
import org.graylog.security.pki.PemUtils;

import java.security.cert.X509Certificate;

public class CollectorTLSUtils {
    private final CollectorCaService caService;
    private final CollectorCaKeyManager keyManager;

    @Inject
    public CollectorTLSUtils(CollectorCaService caService, CollectorCaKeyManager keyManager) {
        this.caService = caService;
        this.keyManager = keyManager;
    }

    /**
     * Creates a new {@link SslContextBuilder} configured for the OTLP server endpoint.
     * <p>
     * The builder is configured with:
     * <ul>
     *   <li>The OTLP server certificate and private key for server identity</li>
     *   <li>Client authentication required (mTLS)</li>
     *   <li>The signing cert as the trust anchor for validating client certificates</li>
     * </ul>
     *
     * @return a configured SslContextBuilder ready to be built
     */
    public SslContextBuilder newServerSslContextBuilder() {
        final var signingCert = caService.getSigningCert();

        try {
            final X509Certificate trustedCert = PemUtils.parseCertificate(signingCert.certificate());

            // The Collector only has access to the CA cert, so we need to have the intermediate signing cert
            // in the key cert chain.
            return SslContextBuilder.forServer(keyManager)
                    // JDK provider required: BoringSSL (OPENSSL) can load Ed25519 keys but cannot
                    // complete TLS handshakes — its cipher suite negotiation doesn't recognize Ed25519.
                    .sslProvider(SslProvider.JDK)
                    .clientAuth(ClientAuth.REQUIRE)
                    .trustManager(trustedCert);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OTLP server SSL context", e);
        }
    }
}
