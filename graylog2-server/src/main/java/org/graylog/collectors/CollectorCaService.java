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

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Service for managing the Collectors CA hierarchy and providing certificate accessors.
 * <p>
 * The CA hierarchy consists of:
 * <ul>
 *   <li>CA cert (Ed25519, 30 years) - self-signed trust anchor</li>
 *   <li>Signing cert (intermediate, 5 years) - signs agent CSRs and issues OTLP server certs</li>
 *   <li>OTLP Server cert (end-entity, 2 years) - TLS server certificate for OTLP ingest endpoint</li>
 * </ul>
 * <p>
 * Certificate IDs are persisted in {@link CollectorsConfig} by the caller.
 */
@Singleton
public class CollectorCaService {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorCaService.class);

    static final Duration CA_CERT_VALIDITY = Duration.ofDays(30 * 365);
    static final Duration SIGNING_CERT_VALIDITY = Duration.ofDays(5 * 365);
    static final Duration OTLP_SERVER_CERT_VALIDITY = Duration.ofDays(2 * 365);

    // Renew when less than 20% of the certificate's lifetime remains
    static final double RENEWAL_THRESHOLD_RATIO = 0.2;

    static final String CA_CERT_CN = "Collectors CA";
    static final String SIGNING_CERT_CN = "Collectors Signing";
    static final String OTLP_SERVER_CERT_CN = "Collectors OTLP Server";

    private final CertificateService certificateService;
    private final ClusterIdService clusterIdService;
    private final CollectorsConfigService collectorsConfigService;
    private final Clock clock;

    @Inject
    public CollectorCaService(CertificateService certificateService,
                              ClusterIdService clusterIdService,
                              CollectorsConfigService collectorsConfigService,
                              Clock clock) {
        this.certificateService = certificateService;
        this.clusterIdService = clusterIdService;
        this.collectorsConfigService = collectorsConfigService;
        this.clock = clock;
    }

    private CollectorsConfig ensureConfig() {
        return collectorsConfigService.get().orElseThrow(() -> new IllegalStateException("Collectors config not found"));
    }

    private IllegalStateException caNotInitializedError() {
        return new IllegalStateException("CA not initialized");
    }

    /**
     * Returns the CA certificate.
     *
     * @return the CA certificate entry
     */
    public CertificateEntry getCaCert() {
        return certificateService.findById(ensureConfig().caCertId()).orElseThrow(this::caNotInitializedError);
    }

    /**
     * Returns the signing certificate for signing agent CSRs.
     *
     * @return the signing certificate entry
     */
    public CertificateEntry getSigningCert() {
        return certificateService.findById(ensureConfig().signingCertId()).orElseThrow(this::caNotInitializedError);
    }

    /**
     * Returns the OTLP server certificate for TLS on the OTLP ingest endpoint.
     *
     * @return the OTLP server certificate entry
     */
    public CertificateEntry getOtlpServerCert() {
        return certificateService.findById(ensureConfig().otlpServerCertId()).orElseThrow(this::caNotInitializedError);
    }

    /**
     * Loads the existing Collector CA hierarchy.
     *
     * @return the CA hierarchy
     */
    public CaHierarchy loadHierarchy() {
        return new CaHierarchy(getCaCert(), getSigningCert(), getOtlpServerCert());
    }

    /**
     * Returns true if the CA is already initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isCaInitialized() {
        final var maybeConfig = collectorsConfigService.get();
        return maybeConfig.isPresent() && isNotBlank(maybeConfig.get().caCertId());
    }

    /**
     * Checks if the signing and OTLP server certificates need renewal and renews them if necessary.
     * <p>
     * The signing cert is renewed when less than {@link #RENEWAL_THRESHOLD_RATIO} of its lifetime remains.
     * When the signing cert is renewed, the OTLP server cert is always re-issued (cascading renewal)
     * because it is signed by the signing cert. The OTLP server cert is also independently checked
     * and renewed if it approaches expiry on its own.
     */
    public void renewCertificates() {
        if (!isCaInitialized()) {
            LOG.debug("CA not initialized - skipping renewal");
            return;
        }

        final var hierarchy = loadHierarchy();
        final var now = Instant.now(clock);

        try {
            final var builder = certificateService.builder();

            if (needsRenewal(hierarchy.signingCert(), now)) {
                final var curSigningCert = hierarchy.signingCert();

                LOG.info("Renewing signing certificate <{}> (expires {})", curSigningCert.fingerprint(), curSigningCert.notAfter());
                final var newSigningCert = certificateService.save(createSigningCert(builder, hierarchy.caCert()));

                // Cascading renewal: re-issue OTLP server cert with the new signing cert
                LOG.info("Re-issuing OTLP server certificate (signing cert renewed)");
                final var newServerCert = certificateService.save(createServerCert(builder, newSigningCert));

                collectorsConfigService.save(ensureConfig().toBuilder()
                        .signingCertId(newSigningCert.id())
                        .otlpServerCertId(newServerCert.id())
                        .build());
            } else {
                final var signingCert = hierarchy.signingCert();
                final var curServerCert = hierarchy.otlpServerCert();

                if (needsRenewal(curServerCert, now)) {
                    LOG.info("Renewing OTLP server certificate <{}> (expires {})", curServerCert.fingerprint(), curServerCert.notAfter());
                    final var newServerCert = certificateService.save(createServerCert(builder, signingCert));
                    collectorsConfigService.save(ensureConfig().toBuilder()
                            .otlpServerCertId(newServerCert.id())
                            .build());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to renew certificates", e);
        }
    }

    /**
     * Returns true if the certificate needs renewal based on its remaining lifetime.
     */
    @VisibleForTesting
    boolean needsRenewal(CertificateEntry cert, Instant now) {
        final var totalLifetime = Duration.between(cert.notBefore(), cert.notAfter());
        final var remaining = Duration.between(now, cert.notAfter());
        return remaining.toMillis() < (long) (totalLifetime.toMillis() * RENEWAL_THRESHOLD_RATIO);
    }

    /**
     * Initialize the CA hierarchy.
     *
     * @return the CA init result
     */
    public CaHierarchy initializeCa() {
        if (isCaInitialized()) {
            throw new IllegalStateException("Collectors CA is already initialized");
        }

        LOG.info("Creating Collectors CA hierarchy");

        try {
            final var builder = certificateService.builder();

            final CertificateEntry caCert = certificateService.save(createRootCert(builder));
            final CertificateEntry signingCert = certificateService.save(createSigningCert(builder, caCert));
            final CertificateEntry serverCert = certificateService.save(createServerCert(builder, signingCert));

            return new CaHierarchy(caCert, signingCert, serverCert);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Collectors CA hierarchy", e);
        }
    }


    private CertificateEntry createRootCert(CertificateBuilder builder) throws Exception {
        return builder.createRootCa(CA_CERT_CN, Algorithm.ED25519, CA_CERT_VALIDITY);
    }

    private CertificateEntry createSigningCert(CertificateBuilder builder, CertificateEntry issuerCert) throws Exception {
        return builder.createIntermediateCa(SIGNING_CERT_CN, issuerCert, SIGNING_CERT_VALIDITY);
    }

    private CertificateEntry createServerCert(CertificateBuilder builder, CertificateEntry issuerCert) throws Exception {
        return builder.createEndEntityCert(
                OTLP_SERVER_CERT_CN,
                issuerCert,
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                KeyPurposeId.id_kp_serverAuth,
                OTLP_SERVER_CERT_VALIDITY,
                List.of(clusterIdService.getString())
        );
    }

    public record CaHierarchy(CertificateEntry caCert,
                              CertificateEntry signingCert,
                              CertificateEntry otlpServerCert) {}
}
