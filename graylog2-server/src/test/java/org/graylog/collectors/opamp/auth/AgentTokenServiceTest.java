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
package org.graylog.collectors.opamp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.opamp.OpAmpCaService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
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
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
class AgentTokenServiceTest {
    private static final String TEST_CLUSTER_ID = "test-cluster-id-12345";

    private CertificateService certificateService;
    private ClusterConfigService clusterConfigService;
    private OpAmpCaService opAmpCaService;
    private CollectorInstanceService collectorInstanceService;
    private AgentTokenService agentTokenService;

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
        when(clusterConfigService.get(ClusterId.class))
                .thenReturn(ClusterId.create(TEST_CLUSTER_ID));
        collectorInstanceService = new CollectorInstanceService(mongoCollections);
        opAmpCaService = new OpAmpCaService(certificateService, clusterConfigService);
        agentTokenService = new AgentTokenService(collectorInstanceService);
    }

    @Test
    void validateAgentTokenReturnsIdentifiedForValidToken() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // Create CA hierarchy
        final CertificateEntry enrollmentCa = opAmpCaService.getOpAmpCa();

        // Generate agent key pair and CSR (use Ed25519 to match CA)
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final byte[] csrPem = createCsrPem("CN=test-agent", agentKeyPair, "Ed25519");

        // Sign CSR with enrollment CA - need to create a CertificateEntry from the signed X509Certificate
        final X509Certificate signedCert = certificateService.builder().signCsr(csrPem, enrollmentCa, "test-agent", Duration.ofDays(365));
        final String certFingerprint = PemUtils.computeFingerprint(signedCert);
        final String certPem = PemUtils.toPem(signedCert);

        // Save agent with cert
        final CollectorInstanceDTO collectorInstanceDTO = collectorInstanceService.enroll(
                "test-instance-uid",
                "test-fleet",
                certFingerprint,
                certPem,
                enrollmentCa.id(),
                Instant.now()
        );

        // Create agent JWT with x5t#S256 header (RFC 7515 format)
        final String agentToken = Jwts.builder()
                .header()
                .add("typ", "agent+jwt")
                .add("x5t#S256", PemUtils.fingerprintToX5t(certFingerprint))
                .and()
                .subject(collectorInstanceDTO.instanceUid())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(agentKeyPair.getPrivate())
                .compact();

        // Validate
        final Optional<OpAmpAuthContext.Identified> result = agentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isPresent();
        assertThat(result.get().instanceUid()).isEqualTo("test-instance-uid");
        assertThat(result.get().transport()).isEqualTo(OpAmpAuthContext.Transport.HTTP);
    }

    @Test
    void validateAgentTokenReturnsEmptyForUnknownFingerprint() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // Generate agent key pair (not enrolled)
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();

        // Create agent JWT with unknown fingerprint (using x5t#S256 format)
        // Use a valid base64url-encoded 32-byte value that won't match any agent
        final String unknownX5t = PemUtils.fingerprintToX5t("sha256:0000000000000000000000000000000000000000000000000000000000000000");
        final String agentToken = Jwts.builder()
                .header()
                .add("typ", "agent+jwt")
                .add("x5t#S256", unknownX5t)
                .and()
                .subject("unknown-agent")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(agentKeyPair.getPrivate())
                .compact();

        // Validate
        final Optional<OpAmpAuthContext.Identified> result = agentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgentTokenReturnsEmptyForMissingX5tHeader() throws Exception {
        // Generate agent key pair
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();

        // Create agent JWT without x5t#S256 header
        final String agentToken = Jwts.builder()
                .header()
                .add("typ", "agent+jwt")
                .and()
                .subject("agent")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(agentKeyPair.getPrivate())
                .compact();

        // Validate
        final Optional<OpAmpAuthContext.Identified> result = agentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgentTokenReturnsEmptyForInvalidSignature() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // Create CA hierarchy
        final CertificateEntry enrollmentCa = opAmpCaService.getOpAmpCa();

        // Generate agent key pair and CSR (use Ed25519 to match CA)
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final byte[] csrPem = createCsrPem("CN=test-agent", agentKeyPair, "Ed25519");

        // Sign CSR with enrollment CA
        final X509Certificate signedCert = certificateService.builder().signCsr(csrPem, enrollmentCa, "test-agent", Duration.ofDays(365));
        final String certFingerprint = PemUtils.computeFingerprint(signedCert);
        final String certPem = PemUtils.toPem(signedCert);

        // Save agent with cert
        final CollectorInstanceDTO collectorInstanceDTO = collectorInstanceService.enroll(
                "test-instance-uid-2",
                "test-fleet",
                certFingerprint,
                certPem,
                enrollmentCa.id(),
                Instant.now()
        );

        // Create JWT signed with a DIFFERENT key
        final KeyPair differentKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final String agentToken = Jwts.builder()
                .header()
                .add("typ", "agent+jwt")
                .add("x5t#S256", PemUtils.fingerprintToX5t(certFingerprint))
                .and()
                .subject(collectorInstanceDTO.instanceUid())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(differentKeyPair.getPrivate())
                .compact();

        // Validate - should fail because signature doesn't match certificate's public key
        final Optional<OpAmpAuthContext.Identified> result = agentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }


    private byte[] createCsrPem(String subject, KeyPair keyPair, String algorithm) throws Exception {
        final X500Name x500Name = new X500Name(subject);
        final ContentSigner signer = new JcaContentSignerBuilder(algorithm)
                .build(keyPair.getPrivate());
        final PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(x500Name, keyPair.getPublic())
                .build(signer);

        final StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(csr);
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }
}
