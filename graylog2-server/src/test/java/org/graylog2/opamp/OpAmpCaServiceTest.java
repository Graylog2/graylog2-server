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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OpAmpCaService}.
 */
@ExtendWith(MongoDBExtension.class)
class OpAmpCaServiceTest {

    private CertificateService certificateService;
    private ClusterConfigService clusterConfigService;
    private OpAmpCaService opAmpCaService;

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
        certificateService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty());
        clusterConfigService = mock(ClusterConfigService.class);
        opAmpCaService = new OpAmpCaService(certificateService, clusterConfigService);
    }

    private void mockClusterConfigStorage() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);
    }

    @Test
    void ensureInitialized_createsFullHierarchy() {
        mockClusterConfigStorage();

        final OpAmpCaService.CaHierarchy hierarchy = opAmpCaService.ensureInitialized();

        assertThat(hierarchy.opAmpCa()).isNotNull();
        assertThat(hierarchy.opAmpCa().id()).isNotNull();
        assertThat(hierarchy.opAmpCa().fingerprint()).startsWith("sha256:");

        assertThat(hierarchy.tokenSigningCert()).isNotNull();
        assertThat(hierarchy.tokenSigningCert().id()).isNotNull();
        assertThat(hierarchy.tokenSigningCert().fingerprint()).startsWith("sha256:");

        assertThat(hierarchy.otlpServerCert()).isNotNull();
        assertThat(hierarchy.otlpServerCert().id()).isNotNull();
        assertThat(hierarchy.otlpServerCert().fingerprint()).startsWith("sha256:");

        // Verify all four certs were saved (root CA + opAmpCa + tokenSigning + otlpServer)
        assertThat(certificateService.findAll()).hasSize(4);
    }

    @Test
    void ensureInitialized_isIdempotent() {
        mockClusterConfigStorage();

        final OpAmpCaService.CaHierarchy first = opAmpCaService.ensureInitialized();
        final OpAmpCaService.CaHierarchy second = opAmpCaService.ensureInitialized();

        assertThat(second.opAmpCa().id()).isEqualTo(first.opAmpCa().id());
        assertThat(second.tokenSigningCert().id()).isEqualTo(first.tokenSigningCert().id());
        assertThat(second.otlpServerCert().id()).isEqualTo(first.otlpServerCert().id());

        // Verify all four certs were saved only once (root CA + opAmpCa + tokenSigning + otlpServer)
        assertThat(certificateService.findAll()).hasSize(4);
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
        final org.bouncycastle.asn1.ASN1OctetString octetString =
                org.bouncycastle.asn1.ASN1OctetString.getInstance(ekuBytes);
        final ExtendedKeyUsage eku = ExtendedKeyUsage.getInstance(
                org.bouncycastle.asn1.ASN1Sequence.getInstance(octetString.getOctets()));
        assertThat(eku.hasKeyPurposeId(KeyPurposeId.id_kp_serverAuth)).isTrue();
    }

    @Test
    void otlpServerCert_hasCorrectCn() throws Exception {
        mockClusterConfigStorage();

        final CertificateEntry otlpServerCert = opAmpCaService.getOtlpServerCert();
        final X509Certificate cert = PemUtils.parseCertificate(otlpServerCert.certificate());

        assertThat(cert.getSubjectX500Principal().getName()).contains("CN=Graylog OpAMP OTLP Server");
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
