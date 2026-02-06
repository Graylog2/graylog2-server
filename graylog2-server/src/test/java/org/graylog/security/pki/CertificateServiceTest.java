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
package org.graylog.security.pki;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.grn.GRNRegistry;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CertificateService} CRUD operations.
 * For certificate creation tests, see {@link CertificateBuilderTest}.
 * For PEM utilities tests, see {@link PemUtilsTest}.
 */
@ExtendWith(MongoDBExtension.class)
class CertificateServiceTest {

    private CertificateService certificateService;
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
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
        certificateService = new CertificateService(mongoCollections, encryptedValueService);
    }

    @Test
    void encryptedValueServiceGetterReturnsInjectedService() {
        assertThat(certificateService.encryptedValueService()).isSameAs(encryptedValueService);
    }

    @Test
    void builderReturnsNewCertificateBuilder() {
        final CertificateBuilder builder = certificateService.builder();
        assertThat(builder).isNotNull();
    }

    @Test
    void saveInsertsCertificateEntryWithNullId() {
        final CertificateEntry entry = createCertificateEntry(null, "SHA256:fingerprint1");

        final CertificateEntry saved = certificateService.save(entry);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.fingerprint()).isEqualTo("SHA256:fingerprint1");
    }

    @Test
    void saveReplacesCertificateEntryWithExistingId() {
        final CertificateEntry original = createCertificateEntry(null, "SHA256:original");
        final CertificateEntry saved = certificateService.save(original);
        final String savedId = saved.id();

        final CertificateEntry updated = new CertificateEntry(
                savedId,
                "SHA256:updated",
                createEncryptedValue(),
                "-----BEGIN CERTIFICATE-----\nUPDATED\n-----END CERTIFICATE-----",
                List.of(),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")
        );

        final CertificateEntry result = certificateService.save(updated);

        assertThat(result.id()).isEqualTo(savedId);
        assertThat(result.fingerprint()).isEqualTo("SHA256:updated");
        assertThat(result.certificate()).isEqualTo("-----BEGIN CERTIFICATE-----\nUPDATED\n-----END CERTIFICATE-----");
    }

    @Test
    void findByIdReturnsEmptyForNonExistentId() {
        final Optional<CertificateEntry> result = certificateService.findById("000000000000000000000000");
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdReturnsSavedCertificateEntry() {
        final CertificateEntry entry = createCertificateEntry(null, "SHA256:findbyid");
        final CertificateEntry saved = certificateService.save(entry);

        final Optional<CertificateEntry> result = certificateService.findById(saved.id());

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(saved.id());
        assertThat(result.get().fingerprint()).isEqualTo("SHA256:findbyid");
    }

    @Test
    void findByFingerprintReturnsEmptyForNonExistentFingerprint() {
        final Optional<CertificateEntry> result = certificateService.findByFingerprint("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    void findByFingerprintReturnsSavedCertificateEntry() {
        final CertificateEntry entry = createCertificateEntry(null, "SHA256:byfingerprint");
        certificateService.save(entry);

        final Optional<CertificateEntry> result = certificateService.findByFingerprint("SHA256:byfingerprint");

        assertThat(result).isPresent();
        assertThat(result.get().fingerprint()).isEqualTo("SHA256:byfingerprint");
    }

    @Test
    void fingerprintIsUnique() {
        final CertificateEntry entry1 = createCertificateEntry(null, "SHA256:unique");
        certificateService.save(entry1);

        final CertificateEntry entry2 = createCertificateEntry(null, "SHA256:unique");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.mongodb.MongoWriteException.class,
                () -> certificateService.save(entry2)
        );
    }

    @Test
    void integrationTestWithBuilder() throws Exception {
        // Test the full workflow: create cert with builder, save, retrieve
        final CertificateEntry rootCa = certificateService.save(
                certificateService.builder().createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(365))
        );

        assertThat(rootCa.id()).isNotNull();
        assertThat(rootCa.fingerprint()).startsWith("sha256:");

        final Optional<CertificateEntry> retrieved = certificateService.findById(rootCa.id());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().fingerprint()).isEqualTo(rootCa.fingerprint());
    }

    private CertificateEntry createCertificateEntry(String id, String fingerprint) {
        return new CertificateEntry(
                id,
                fingerprint,
                createEncryptedValue(),
                "-----BEGIN CERTIFICATE-----\nTEST\n-----END CERTIFICATE-----",
                List.of("-----BEGIN CERTIFICATE-----\nISSUER\n-----END CERTIFICATE-----"),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }

    private EncryptedValue createEncryptedValue() {
        return EncryptedValue.builder()
                .value("2d043f9a7d5a5a7537d3e93c93c5dc40")
                .salt("c93c0263bfc3713d")
                .isKeepValue(false)
                .isDeleteValue(false)
                .build();
    }
}
