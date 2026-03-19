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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
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
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bouncycastle.asn1.ASN1OctetString.getInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OpAmpCaService}.
 */
@ExtendWith(MongoDBExtension.class)
class OpAmpCaServiceTest {

    private CertificateService certificateService;
    private CollectorsConfigService collectorsConfigService;
    private ClusterIdService clusterIdService;
    private OpAmpCaService opAmpCaService;
    private Clock clock = TestClocks.fixedEpoch();

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
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
        collectorsConfigService = mock(CollectorsConfigService.class);
        opAmpCaService = new OpAmpCaService(certificateService, clusterIdService, collectorsConfigService);
    }

    private void mockClusterConfigStorage() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
    }

    @Test
    void ensureInitialized_createsFullHierarchy() {
        mockClusterConfigStorage();

        final OpAmpCaService.CaHierarchy hierarchy = opAmpCaService.ensureInitialized();

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
    void ensureInitialized_isIdempotent() {
        mockClusterConfigStorage();

        final OpAmpCaService.CaHierarchy first = opAmpCaService.ensureInitialized();
        final OpAmpCaService.CaHierarchy second = opAmpCaService.ensureInitialized();

        assertThat(second.caCert().id()).isEqualTo(first.caCert().id());
        assertThat(second.signingCert().id()).isEqualTo(first.signingCert().id());
        assertThat(second.otlpServerCert().id()).isEqualTo(first.otlpServerCert().id());

        // Verify all four certs were saved only once (root CA + opAmpCa + otlpServer)
        assertThat(certificateService.findAll()).hasSize(3);
    }

    private String getCN(CertificateEntry entry) throws Exception {
        return PemUtils.parseCertificate(entry.certificate()).getSubjectX500Principal().getName();
    }

    @Test
    void checkCaCert() throws Exception {
        mockClusterConfigStorage();

        final var cert = opAmpCaService.getCaCert();

        assertThat(getCN(cert)).isEqualTo("O=Graylog,CN=Collectors CA");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getSigAlgName()).isEqualTo("Ed25519");
    }

    @Test
    void checkSigningCert() throws Exception {
        mockClusterConfigStorage();

        final var cert = opAmpCaService.getSigningCert();

        assertThat(getCN(cert)).isEqualTo("O=Graylog,CN=Collectors Signing");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getSigAlgName()).isEqualTo("Ed25519");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getNotAfter()).isEqualTo(Clock.offset(clock, Duration.ofDays(5 * 365)).instant());
    }

    @Test
    void checkOTLPServerCert() throws Exception {
        mockClusterConfigStorage();

        final var cert = opAmpCaService.getOtlpServerCert();

        assertThat(getCN(cert)).isEqualTo("O=Graylog,CN=Collectors OTLP Server");
        assertThat(PemUtils.parseCertificate(cert.certificate()).getSigAlgName()).isEqualTo("Ed25519");
    }

    @Test
    void otlpServerCert_hasServerAuthEku() throws Exception {
        mockClusterConfigStorage();

        final CertificateEntry otlpServerCert = opAmpCaService.getOtlpServerCert();
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
        mockClusterConfigStorage();

        final CertificateEntry otlpServerCert = opAmpCaService.getOtlpServerCert();
        final X509Certificate cert = PemUtils.parseCertificate(otlpServerCert.certificate());

        final var sans = cert.getSubjectAlternativeNames();
        assertThat(sans).isNotNull();
        // GeneralName type 2 = dNSName
        assertThat(sans).anyMatch(entry -> (int) entry.get(0) == 2 && testClusterId.equals(entry.get(1)));
    }

    @Test
    void newServerSslContextBuilder_returnsConfiguredBuilder() throws Exception {
        mockClusterConfigStorage();

        final SslContextBuilder builder = opAmpCaService.newServerSslContextBuilder();
        assertThat(builder).isNotNull();

        // Verify the builder can actually build an SslContext
        final SslContext sslContext = builder.build();
        assertThat(sslContext).isNotNull();
        assertThat(sslContext.isServer()).isTrue();
    }
}
