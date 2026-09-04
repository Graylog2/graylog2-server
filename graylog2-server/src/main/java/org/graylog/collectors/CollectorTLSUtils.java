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

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.net.ssl.SSLException;
import java.util.concurrent.Executor;

@Singleton
public class CollectorTLSUtils {
    private final CollectorCaKeyManager keyManager;
    private final CollectorCaTrustManager trustManager;
    private final Executor certVerificationExecutor;

    @Inject
    public CollectorTLSUtils(CollectorCaKeyManager keyManager,
                             CollectorCaTrustManager trustManager,
                             @CollectorCertVerificationExecutor Executor certVerificationExecutor) {
        this.keyManager = keyManager;
        this.trustManager = trustManager;
        this.certVerificationExecutor = certVerificationExecutor;
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
        try {
            // The Collector only has access to the CA cert, so we need to have the intermediate signing cert
            // in the key cert chain.
            return SslContextBuilder.forServer(keyManager)
                    // JDK provider required: BoringSSL (OPENSSL) can load Ed25519 keys but cannot
                    // complete TLS handshakes — its cipher suite negotiation doesn't recognize Ed25519.
                    .sslProvider(SslProvider.JDK)
                    .clientAuth(ClientAuth.REQUIRE)
                    .trustManager(trustManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OTLP server SSL context", e);
        }
    }

    /**
     * Creates an {@link SslHandler} for the OTLP server endpoint whose TLS handshake delegated
     * tasks run on the {@link CollectorCertVerificationExecutor} rather than the Netty event loop.
     * <p>
     * The JDK {@code SSLEngine} performs client-certificate validation
     * ({@link CollectorCaTrustManager#checkClientTrusted}) as a delegated task; passing the executor
     * to {@code newHandler} makes Netty run those tasks — including any MongoDB-backed instance
     * binding lookup — off the event loop. Prefer this over building the context and creating the
     * handler directly, so the executor is never accidentally omitted.
     *
     * @param alloc the allocator for the new handler
     * @return an {@link SslHandler} configured for mTLS with off-loop handshake task execution
     */
    public SslHandler newServerSslHandler(ByteBufAllocator alloc) throws SSLException {
        return newServerSslContextBuilder().build().newHandler(alloc, certVerificationExecutor);
    }
}
