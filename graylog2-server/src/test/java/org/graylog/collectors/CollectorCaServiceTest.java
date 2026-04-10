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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog.testing.cluster.ClusterConfigServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.bouncycastle.asn1.ASN1OctetString.getInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CollectorCaService}.
 */
@ExtendWith(MongoDBExtension.class)
@ExtendWith(ClusterConfigServiceExtension.class)
class CollectorCaServiceTest {

    private CertificateService certificateService;
    private CollectorsConfigService collectorsConfigService;
    private ClusterIdService clusterIdService;
    private CollectorCaService collectorCaService;
    private Clock clock = TestClocks.fixedEpoch();
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb, ClusterConfigService clusterConfigService) {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        final ObjectMapper objectMapper = new ObjectMapperProvider(
                ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                encryptedValueService,
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig()
        ).get();

        final MongoCollections mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper),
                mongodb.mongoConnection()
        );
        certificateService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty(), clock);
        clusterIdService = mock(ClusterIdService.class);
        when(clusterIdService.getString()).thenReturn("cluster-id");
        final var httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(java.net.URI.create("https://localhost:443/"));
        collectorsConfigService = new CollectorsConfigService(clusterConfigService, mock(ClusterEventBus.class), httpConfiguration);
        collectorCaService = new CollectorCaService(certificateService, clusterIdService, collectorsConfigService, clock);
    }

    private void initConfig() {
        initConfig(collectorCaService.initializeCa());
    }

    private void initConfig(CollectorCaService.CaHierarchy hierarchy) {
        collectorsConfigService.save(CollectorsConfig.createDefaultBuilder("localhost")
                .caCertId(hierarchy.caCert().id())
                .signingCertId(hierarchy.signingCert().id())
                .otlpServerCertId(hierarchy.otlpServerCert().id())
                .build());
    }

    @Test
    void initializeCa() {
        final var hierarchy = collectorCaService.initializeCa();

        assertThat(hierarchy.caCert()).isNotNull();
        assertThat(hierarchy.caCert().id()).isNotNull();
        assertThat(hierarchy.caCert().fingerprint()).startsWith("sha256:");

        assertThat(hierarchy.signingCert()).isNotNull();
        assertThat(hierarchy.signingCert().id()).isNotNull();
        assertThat(hierarchy.signingCert().fingerprint()).startsWith("sha256:");

        assertThat(hierarchy.otlpServerCert()).isNotNull();
        assertThat(hierarchy.otlpServerCert().id()).isNotNull();
        assertThat(hierarchy.otlpServerCert().fingerprint()).startsWith("sha256:");

        // Verify all four certs were saved (root CA + opAmpCa + otlpServer)
        assertThat(certificateService.findAll()).hasSize(3);
    }

    @Test
    void initializeCaFailsWhenConfigExists() {
        final CollectorCaService.CaHierarchy first = collectorCaService.initializeCa();
        initConfig(first);

        assertThatThrownBy(() -> collectorCaService.initializeCa()).isInstanceOf(IllegalStateException.class);

        // Verify all four certs were saved only once (root CA + opAmpCa + otlpServer)
        assertThat(certificateService.findAll()).hasSize(3);
    }

    private String getCN(CertificateEntry entry) throws Exception {
        return PemUtils.parseCertificate(entry.certificate()).getSubjectX500Principal().getName();
    }

    @Test
    void checkCaCert() throws Exception {
        initConfig();
        final var cert = collectorCaService.getCaCert();

        assertThat(getCN(cert)).isEqualTo("O=Graylog,CN=Collectors CA");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getSigAlgName()).isEqualTo("Ed25519");
    }

    @Test
    void checkSigningCert() throws Exception {
        initConfig();
        final var cert = collectorCaService.getSigningCert();

        assertThat(getCN(cert)).isEqualTo("O=Graylog,CN=Collectors Signing");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getSigAlgName()).isEqualTo("Ed25519");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getNotAfter()).isEqualTo(Clock.offset(clock, Duration.ofDays(5 * 365)).instant());
    }

    @Test
    void checkOTLPServerCert() throws Exception {
        initConfig();
        final var cert = collectorCaService.getOtlpServerCert();

        assertThat(getCN(cert)).isEqualTo("O=Graylog,CN=Collectors OTLP Server");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getSigAlgName()).isEqualTo("Ed25519");
    }

    @Test
    void otlpServerCert_hasServerAuthEku() throws Exception {
        initConfig();
        final CertificateEntry otlpServerCert = collectorCaService.getOtlpServerCert();
        final X509Certificate cert = PemUtils.parseCertificate(otlpServerCert.certificate());

        // OID 1.3.6.1.5.5.7.3.1 = id-kp-serverAuth
        final byte[] ekuBytes = cert.getExtensionValue(Extension.extendedKeyUsage.getId());
        assertThat(ekuBytes).isNotNull();

        // Parse the EKU extension
        final ASN1OctetString octetString = getInstance(ekuBytes);
        final ExtendedKeyUsage eku = ExtendedKeyUsage.getInstance(ASN1Sequence.getInstance(octetString.getOctets()));

        assertThat(eku.hasKeyPurposeId(KeyPurposeId.id_kp_serverAuth)).isTrue();
    }

    @Test
    void otlpServerCert_hasClusterIdAsDnsSan() throws Exception {
        final String testClusterId = "2209F727-F7E1-4123-9386-94FE3B354A07";
        when(clusterIdService.getString()).thenReturn(testClusterId);
        initConfig();

        final CertificateEntry otlpServerCert = collectorCaService.getOtlpServerCert();
        final X509Certificate cert = PemUtils.parseCertificate(otlpServerCert.certificate());

        final var sans = cert.getSubjectAlternativeNames();
        assertThat(sans).isNotNull();
        // GeneralName type 2 = dNSName
        assertThat(sans).anyMatch(entry -> (int) entry.get(0) == 2 && testClusterId.equals(entry.get(1)));
    }

    @Test
    void needsRenewal_returnsFalseWhenCertIsNew() {
        initConfig();
        final var signingCert = collectorCaService.getSigningCert();

        // At epoch (cert creation time), the full lifetime remains
        assertThat(collectorCaService.needsRenewal(signingCert, Instant.EPOCH)).isFalse();
    }

    @Test
    void needsRenewal_returnsTrueWhenBelowThreshold() {
        initConfig();
        final var signingCert = collectorCaService.getSigningCert();

        // Advance to 85% of signing cert lifetime (5 years) — only 15% remains, below 20% threshold
        final var signingLifetimeDays = 5 * 365;
        final var at85Percent = Instant.EPOCH.plus(Duration.ofDays((long) (signingLifetimeDays * 0.85)));
        assertThat(collectorCaService.needsRenewal(signingCert, at85Percent)).isTrue();
    }

    @Test
    void needsRenewal_returnsFalseWhenAboveThreshold() {
        initConfig();
        final var signingCert = collectorCaService.getSigningCert();

        // Advance to 50% of signing cert lifetime — 50% remains, above 20% threshold
        final var signingLifetimeDays = 5 * 365;
        final var atHalfway = Instant.EPOCH.plus(Duration.ofDays((long) (signingLifetimeDays * 0.5)));
        assertThat(collectorCaService.needsRenewal(signingCert, atHalfway)).isFalse();
    }

    @Test
    void renewCertificates_doesNothingWhenCertsAreNew() {
        initConfig();
        final var configBefore = collectorsConfigService.get().orElseThrow();

        collectorCaService.renewCertificates();

        final var configAfter = collectorsConfigService.get().orElseThrow();
        assertThat(configAfter.signingCertId()).isEqualTo(configBefore.signingCertId());
        assertThat(configAfter.otlpServerCertId()).isEqualTo(configBefore.otlpServerCertId());
    }

    @Test
    void renewCertificates_renewsSigningCertAndCascadesToServerCert() {
        initConfig();
        final var configBefore = collectorsConfigService.get().orElseThrow();

        // Create a service with a clock past the signing cert's renewal threshold
        final var signingLifetimeDays = 5 * 365;
        final var futureClock = Clock.fixed(
                Instant.EPOCH.plus(Duration.ofDays((long) (signingLifetimeDays * 0.85))),
                ZoneOffset.UTC);
        final var futureService = new CollectorCaService(certificateService, clusterIdService, collectorsConfigService, futureClock);

        futureService.renewCertificates();

        final var configAfter = collectorsConfigService.get().orElseThrow();
        // Both signing and server cert should have changed
        assertThat(configAfter.signingCertId()).isNotEqualTo(configBefore.signingCertId());
        assertThat(configAfter.otlpServerCertId()).isNotEqualTo(configBefore.otlpServerCertId());
        // CA cert should remain unchanged
        assertThat(configAfter.caCertId()).isEqualTo(configBefore.caCertId());

        // Verify the new signing cert is signed by the CA
        final var newSigningCert = certificateService.findById(configAfter.signingCertId()).orElseThrow();
        final var caCert = certificateService.findById(configAfter.caCertId()).orElseThrow();
        assertThat(newSigningCert.authorityKeyIdentifier()).hasValue(caCert.subjectKeyIdentifier());

        // Verify the new server cert is signed by the new signing cert
        final var newServerCert = certificateService.findById(configAfter.otlpServerCertId()).orElseThrow();
        assertThat(newServerCert.authorityKeyIdentifier()).hasValue(newSigningCert.subjectKeyIdentifier());
    }

    @Test
    void renewCertificates_renewsOnlyServerCertWhenSigningCertIsFresh() {
        initConfig();
        final var configBefore = collectorsConfigService.get().orElseThrow();

        // Advance past the OTLP server cert threshold (2 years) but not the signing cert threshold (5 years)
        final var serverLifetimeDays = 2 * 365;
        final var futureClock = Clock.fixed(
                Instant.EPOCH.plus(Duration.ofDays((long) (serverLifetimeDays * 0.85))),
                ZoneOffset.UTC);
        final var futureService = new CollectorCaService(certificateService, clusterIdService, collectorsConfigService, futureClock);

        futureService.renewCertificates();

        final var configAfter = collectorsConfigService.get().orElseThrow();
        // Only server cert should have changed
        assertThat(configAfter.caCertId()).isEqualTo(configBefore.caCertId());
        assertThat(configAfter.signingCertId()).isEqualTo(configBefore.signingCertId());
        assertThat(configAfter.otlpServerCertId()).isNotEqualTo(configBefore.otlpServerCertId());

        // Verify the new server cert is signed by the existing signing cert
        final var newServerCert = certificateService.findById(configAfter.otlpServerCertId()).orElseThrow();
        final var signingCert = certificateService.findById(configAfter.signingCertId()).orElseThrow();
        assertThat(newServerCert.authorityKeyIdentifier()).hasValue(signingCert.subjectKeyIdentifier());
    }

    @Test
    void renewCertificates_skipsWhenCaNotInitialized() {
        // No initConfig() call — CA is not initialized
        collectorCaService.renewCertificates();
        // Should return silently without errors
        assertThat(collectorsConfigService.get()).isEmpty();
    }

    @Test
    void newServerSslContextBuilder_returnsConfiguredBuilder() throws Exception {
        initConfig();
        final var cache = new CollectorCaCache(collectorCaService, certificateService, encryptedValueService, new EventBus(), TestClocks.fixedEpoch());
        final var tlsUtils = new CollectorTLSUtils(new CollectorCaKeyManager(cache), new CollectorCaTrustManager(cache, clock));
        final SslContextBuilder builder = tlsUtils.newServerSslContextBuilder();
        assertThat(builder).isNotNull();

        // Verify the builder can actually build an SslContext
        final SslContext sslContext = builder.build();
        assertThat(sslContext).isNotNull();
        assertThat(sslContext.isServer()).isTrue();
    }
}
