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
package org.graylog.collectors.opamp;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.rest.CollectorsConfigResource;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Service for managing the Collectors CA hierarchy and providing certificate accessors.
 * <p>
 * The CA hierarchy consists of:
 * <ul>
 *   <li>CA cert (Ed25519, 30 years) - self-signed trust anchor</li>
 *   <li>Signing cert (intermediate, 5 years) - signs agent CSRs and issues OTLP server certs</li>
 *   <li>Token signing cert (end-entity, 2 years) - signs enrollment JWTs</li>
 *   <li>OTLP Server cert (end-entity, 2 years) - TLS server certificate for OTLP ingest endpoint</li>
 * </ul>
 * <p>
 * The hierarchy is created on-demand when first accessed via {@link #ensureInitialized()}.
 * Certificate IDs are persisted in {@link CollectorsConfig} by the caller
 * ({@link CollectorsConfigResource}).
 */
@Singleton
public class OpAmpCaService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpCaService.class);

    static final Duration CA_CERT_VALIDITY = Duration.ofDays(30 * 365);
    static final Duration SIGNING_CERT_VALIDITY = Duration.ofDays(5 * 365);
    static final Duration OTLP_SERVER_CERT_VALIDITY = Duration.ofDays(2 * 365);

    static final String CA_CERT_CN = "Collectors CA";
    static final String SIGNING_CERT_CN = "Collectors Signing";
    static final String OTLP_SERVER_CERT_CN = "Collectors OTLP Server";

    private final CertificateService certificateService;
    private final ClusterIdService clusterIdService;
    private final CollectorsConfigService collectorsConfigService;

    private volatile CaHierarchy cachedHierarchy;

    @Inject
    public OpAmpCaService(CertificateService certificateService,
                          ClusterIdService clusterIdService,
                          CollectorsConfigService collectorsConfigService) {
        this.certificateService = certificateService;
        this.clusterIdService = clusterIdService;
        this.collectorsConfigService = collectorsConfigService;
    }

    /**
     * Returns the CA certificate.
     *
     * @return the CA certificate entry
     */
    public CertificateEntry getCaCert() {
        return ensureInitialized().caCert();
    }

    /**
     * Returns the signing certificate for signing agent CSRs.
     *
     * @return the signing certificate entry
     */
    public CertificateEntry getSigningCert() {
        return ensureInitialized().signingCert();
    }

    /**
     * Returns the OTLP server certificate for TLS on the OTLP ingest endpoint.
     *
     * @return the OTLP server certificate entry
     */
    public CertificateEntry getOtlpServerCert() {
        return ensureInitialized().otlpServerCert();
    }

    public String getCaCertId() {
        return ensureInitialized().caCert().id();
    }

    public String getSigningCertId() {
        return ensureInitialized().signingCert().id();
    }

    public String getOtlpServerCertId() {
        return ensureInitialized().otlpServerCert().id();
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
        ensureInitialized();
        final CertificateEntry otlpServerCert = getOtlpServerCert();
        final CertificateEntry signingCert = getSigningCert();

        try {
            final PrivateKey key = PemUtils.parsePrivateKey(certificateService.encryptedValueService().decrypt(otlpServerCert.privateKey()));

            final X509Certificate signingCertPem = PemUtils.parseCertificate(signingCert.certificate());
            final X509Certificate serverCertPem = PemUtils.parseCertificate(otlpServerCert.certificate());
            final X509Certificate trustedCert = PemUtils.parseCertificate(signingCert.certificate());

            // The Collector only has access to the CA cert, so we need to have the intermediate signing cert
            // in the key cert chain.
            return SslContextBuilder.forServer(key, serverCertPem, signingCertPem)
                    // JDK provider required: BoringSSL (OPENSSL) can load Ed25519 keys but cannot
                    // complete TLS handshakes — its cipher suite negotiation doesn't recognize Ed25519.
                    .sslProvider(SslProvider.JDK)
                    .clientAuth(ClientAuth.REQUIRE)
                    .trustManager(trustedCert);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OTLP server SSL context", e);
        }
    }

    /**
     * Ensures the CA hierarchy is initialized, creating it if necessary.
     * <p>
     * Handles two scenarios:
     * <ol>
     *   <li>Full config exists in {@link CollectorsConfig} — loads from stored cert IDs</li>
     *   <li>No config exists — creates the full hierarchy from scratch</li>
     * </ol>
     * <p>
     * Results are cached in memory for the lifetime of this singleton. The caller
     * is responsible for persisting cert IDs into {@link CollectorsConfig}.
     *
     * @return the loaded CA hierarchy
     */
    public CaHierarchy ensureInitialized() {
        if (cachedHierarchy != null) {
            return cachedHierarchy;
        }

        final var maybeConfig = collectorsConfigService.get();
        if (maybeConfig.isPresent()) {
            final var config = maybeConfig.get();

            if (nonNull(config.caCertId()) && nonNull(config.signingCertId()) && nonNull(config.otlpServerCertId())) {
                cachedHierarchy = loadFromConfig(config);
                return cachedHierarchy;
            }
        }

        LOG.info("Creating Collectors CA hierarchy");

        try {
            final var builder = certificateService.builder();

            final CertificateEntry caCert = certificateService.save(
                    builder.createRootCa(CA_CERT_CN, Algorithm.ED25519, CA_CERT_VALIDITY));
            final CertificateEntry signingCert = certificateService.save(
                    builder.createIntermediateCa(SIGNING_CERT_CN, caCert, SIGNING_CERT_VALIDITY));
            final CertificateEntry otlpServerCert = certificateService.save(
                    builder.createEndEntityCert(
                            OTLP_SERVER_CERT_CN,
                            signingCert,
                            KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                            KeyPurposeId.id_kp_serverAuth,
                            OTLP_SERVER_CERT_VALIDITY,
                            List.of(clusterIdService.getString())
                    ));

            cachedHierarchy = new CaHierarchy(caCert, signingCert, otlpServerCert);
            return cachedHierarchy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Collectors CA hierarchy", e);
        }
    }

    private CaHierarchy loadFromConfig(CollectorsConfig config) {
        final CertificateEntry caCert = certificateService.findById(config.caCertId())
                .orElseThrow(() -> new IllegalStateException("CA cert not found: " + config.caCertId()));
        final CertificateEntry signingCert = certificateService.findById(config.signingCertId())
                .orElseThrow(() -> new IllegalStateException("Signing cert not found: " + config.signingCertId()));
        final CertificateEntry otlpServerCert = certificateService.findById(config.otlpServerCertId())
                .orElseThrow(() -> new IllegalStateException("OTLP server cert not found: " + config.otlpServerCertId()));
        return new CaHierarchy(caCert, signingCert, otlpServerCert);
    }

    public record CaHierarchy(CertificateEntry caCert,
                              CertificateEntry signingCert,
                              CertificateEntry otlpServerCert) {}
}
