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
package org.graylog2.opamp;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Service for managing the OpAMP CA hierarchy and providing certificate accessors.
 * <p>
 * The CA hierarchy consists of:
 * <ul>
 *   <li>Root CA (Ed25519, 30 years) - self-signed trust anchor</li>
 *   <li>OpAMP CA (intermediate, 5 years) - signs agent CSRs and issues OTLP server certs</li>
 *   <li>Token Signing cert (end-entity, 2 years) - signs enrollment JWTs</li>
 *   <li>OTLP Server cert (end-entity, 2 years) - TLS server certificate for OTLP ingest endpoint</li>
 * </ul>
 * <p>
 * The hierarchy is created on-demand when first accessed via {@link #ensureInitialized()}.
 * Certificate IDs are persisted in {@link CollectorsConfig} by the caller
 * ({@link org.graylog.collectors.CollectorsConfigResource}).
 */
@Singleton
public class OpAmpCaService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpCaService.class);

    static final Duration ROOT_CA_VALIDITY = Duration.ofDays(30 * 365);
    static final Duration OPAMP_CA_VALIDITY = Duration.ofDays(5 * 365);
    static final Duration TOKEN_SIGNING_VALIDITY = Duration.ofDays(2 * 365);
    static final Duration OTLP_SERVER_VALIDITY = Duration.ofDays(2 * 365);

    static final String ROOT_CA_CN = "Graylog OpAMP Root CA";
    static final String OPAMP_CA_CN = "Graylog OpAMP CA";
    static final String TOKEN_SIGNING_CN = "Graylog OpAMP Token Signing";
    static final String OTLP_SERVER_CN = "Graylog OpAMP OTLP Server";

    private final CertificateService certificateService;
    private final ClusterConfigService clusterConfigService;

    private volatile CaHierarchy cachedHierarchy;

    @Inject
    public OpAmpCaService(CertificateService certificateService,
                          ClusterConfigService clusterConfigService) {
        this.certificateService = certificateService;
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * Returns the OpAMP CA certificate for signing agent CSRs.
     *
     * @return the OpAMP CA certificate entry
     */
    public CertificateEntry getOpAmpCa() {
        return ensureInitialized().opAmpCa();
    }

    /**
     * Returns the token signing certificate for signing enrollment JWTs.
     *
     * @return the token signing certificate entry
     */
    public CertificateEntry getTokenSigningCert() {
        return ensureInitialized().tokenSigningCert();
    }

    /**
     * Returns the OTLP server certificate for TLS on the OTLP ingest endpoint.
     *
     * @return the OTLP server certificate entry
     */
    public CertificateEntry getOtlpServerCert() {
        return ensureInitialized().otlpServerCert();
    }

    public String getOpAmpCaId() {
        return ensureInitialized().opAmpCa().id();
    }

    public String getTokenSigningCertId() {
        return ensureInitialized().tokenSigningCert().id();
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
     *   <li>The OpAMP CA as the trust anchor for validating client certificates</li>
     * </ul>
     *
     * @return a configured SslContextBuilder ready to be built
     */
    public SslContextBuilder newServerSslContextBuilder() {
        ensureInitialized();
        final CertificateEntry otlpServerCert = getOtlpServerCert();
        final CertificateEntry opAmpCa = getOpAmpCa();

        try {
            final PrivateKey key = PemUtils.parsePrivateKey(
                    certificateService.encryptedValueService().decrypt(otlpServerCert.privateKey()));
            final X509Certificate cert = PemUtils.parseCertificate(otlpServerCert.certificate());
            final X509Certificate caCert = PemUtils.parseCertificate(opAmpCa.certificate());

            return SslContextBuilder.forServer(key, cert)
                    .sslProvider(SslProvider.JDK)
                    .clientAuth(ClientAuth.REQUIRE)
                    .trustManager(caCert);
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

        final CollectorsConfig config = clusterConfigService.get(CollectorsConfig.class);
        if (config != null && config.opampCaId() != null && config.otlpServerCertId() != null) {
            cachedHierarchy = loadFromConfig(config);
            return cachedHierarchy;
        }

        LOG.info("Creating OpAMP CA hierarchy");

        try {
            final var builder = certificateService.builder();

            final CertificateEntry rootCa = certificateService.save(
                    builder.createRootCa(ROOT_CA_CN, Algorithm.ED25519, ROOT_CA_VALIDITY));
            final CertificateEntry opAmpCa = certificateService.save(
                    builder.createIntermediateCa(OPAMP_CA_CN, rootCa, OPAMP_CA_VALIDITY));
            final CertificateEntry tokenSigningCert = certificateService.save(
                    builder.createEndEntityCert(TOKEN_SIGNING_CN, opAmpCa, KeyUsage.digitalSignature, TOKEN_SIGNING_VALIDITY));
            final CertificateEntry otlpServerCert = certificateService.save(
                    builder.createEndEntityCert(OTLP_SERVER_CN, opAmpCa,
                            KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                            KeyPurposeId.id_kp_serverAuth, OTLP_SERVER_VALIDITY));

            cachedHierarchy = new CaHierarchy(opAmpCa, tokenSigningCert, otlpServerCert);
            return cachedHierarchy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OpAMP CA hierarchy", e);
        }
    }

    private CaHierarchy loadFromConfig(CollectorsConfig config) {
        final CertificateEntry opAmpCa = certificateService.findById(config.opampCaId())
                .orElseThrow(() -> new IllegalStateException("OpAMP CA not found: " + config.opampCaId()));
        final CertificateEntry tokenSigningCert = certificateService.findById(config.tokenSigningCertId())
                .orElseThrow(() -> new IllegalStateException("Token signing cert not found: " + config.tokenSigningCertId()));
        final CertificateEntry otlpServerCert = certificateService.findById(config.otlpServerCertId())
                .orElseThrow(() -> new IllegalStateException("OTLP server cert not found: " + config.otlpServerCertId()));
        return new CaHierarchy(opAmpCa, tokenSigningCert, otlpServerCert);
    }

    public record CaHierarchy(CertificateEntry opAmpCa, CertificateEntry tokenSigningCert, CertificateEntry otlpServerCert) {}
}
