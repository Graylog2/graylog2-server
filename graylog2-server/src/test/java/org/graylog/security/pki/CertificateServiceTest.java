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
import com.mongodb.MongoWriteException;
import org.graylog.grn.GRNRegistry;
import org.graylog.testing.TestClocks;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        certificateService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty(), TestClocks.fixedEpoch());
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
    void insertInsertsCertificateEntryWithNullId() throws Exception {
        final CertificateEntry entry = createCertificateEntry();

        final CertificateEntry saved = certificateService.insert(entry);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.fingerprint()).isEqualTo(entry.fingerprint());
    }

    @Test
    void insertCertificateWithExistingIdFails() throws Exception {
        final CertificateEntry original = createCertificateEntry();
        final CertificateEntry saved = certificateService.insert(original);

        assertThatThrownBy(() -> certificateService.insert(saved)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByIdReturnsEmptyForNonExistentId() {
        final Optional<CertificateEntry> result = certificateService.findById("000000000000000000000000");
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdReturnsSavedCertificateEntry() throws Exception {
        final CertificateEntry entry = createCertificateEntry();
        final CertificateEntry saved = certificateService.insert(entry);

        final Optional<CertificateEntry> result = certificateService.findById(saved.id());

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(saved.id());
        assertThat(result.get().fingerprint()).isEqualTo(entry.fingerprint());
    }

    @Test
    void findByFingerprintReturnsEmptyForNonExistentFingerprint() {
        final Optional<CertificateEntry> result = certificateService.findByFingerprint("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    void findByFingerprintReturnsSavedCertificateEntry() throws Exception {
        final CertificateEntry entry = certificateService.insert(createCertificateEntry());

        final Optional<CertificateEntry> result = certificateService.findByFingerprint(entry.fingerprint());

        assertThat(result).get().satisfies(found -> assertThat(found.id()).isEqualTo(entry.id()));
    }

    @Test
    void fingerprintIsUnique() throws Exception {
        final CertificateEntry entry = createCertificateEntry();
        certificateService.insert(entry);

        assertThatThrownBy(() -> certificateService.insert(entry.withId(null)))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void saveExtractsDnForIntermediateCa() throws Exception {
        final CertificateEntry rootCa = certificateService.insert(
                certificateService.builder().createRootCa("Root", Algorithm.ED25519, Duration.ofDays(365)));

        final CertificateEntry intermediate = certificateService.builder()
                .createIntermediateCa("Intermediate", rootCa, Duration.ofDays(180));
        final CertificateEntry saved = certificateService.insert(intermediate);

        assertThat(saved.subjectDn()).contains("Intermediate");
        assertThat(saved.issuerDn()).contains("Root");
    }

    @Test
    void findBySubjectKeyIdentifierReturnsEmptyForNonExistentSki() {
        assertThat(certificateService.findBySubjectKeyIdentifier("nonexistent")).isEmpty();
    }

    @Test
    void findBySubjectKeyIdentifierReturnsSavedCertificateEntry() throws Exception {
        final CertificateEntry saved = certificateService.insert(
                certificateService.builder().createRootCa("SKI Test CA", Algorithm.ED25519, Duration.ofDays(365)));

        final var result = certificateService.findBySubjectKeyIdentifier(saved.subjectKeyIdentifier());

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(saved.id());
        assertThat(result.get().fingerprint()).isEqualTo(saved.fingerprint());
    }

    @Test
    void insertReturnsInsertedEntries() throws Exception {
        final var newEntry1 = createCertificateEntry(null, "1");
        final var newEntry2 = createCertificateEntry(null, "2");
        final var newEntry3 = createCertificateEntry(null, "3");
        final List<CertificateEntry> entries = List.of(newEntry1, newEntry2, newEntry3);

        final var insertedEntries = certificateService.insert(entries);

        assertThat(insertedEntries).hasSize(3);

        assertThat(insertedEntries).satisfiesExactly(
                entry1 -> assertThat(entry1.subjectDn()).isEqualTo("O=Graylog,CN=1"),
                entry2 -> assertThat(entry2.subjectDn()).isEqualTo("O=Graylog,CN=2"),
                entry3 -> assertThat(entry3.subjectDn()).isEqualTo("O=Graylog,CN=3")
        );

        assertThat(certificateService.findAll()).containsAll(insertedEntries);
    }

    @Test
    void insertRejectsEntriesWithId() throws Exception {
        final List<CertificateEntry> entries = List.of(
                createCertificateEntry(null, "1"),
                createCertificateEntry("64a7f8b9c0d1e2f3a4b5c6d7", "2")
        );

        assertThatThrownBy(() -> certificateService.insert(entries)).isInstanceOf(IllegalArgumentException.class);

        assertThat(certificateService.findAll()).isEmpty();
    }

    @Test
    void insertEmptyCollectionReturnsZero() {
        assertThat(certificateService.insert(List.of())).isEmpty();
    }

    @Test
    void integrationTestWithBuilder() throws Exception {
        // Test the full workflow: create cert with builder, save, retrieve
        final CertificateEntry rootCa = certificateService.insert(
                certificateService.builder().createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(365))
        );

        assertThat(rootCa.id()).isNotNull();
        assertThat(rootCa.fingerprint()).startsWith("sha256:");

        final Optional<CertificateEntry> retrieved = certificateService.findById(rootCa.id());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().fingerprint()).isEqualTo(rootCa.fingerprint());
    }

    private CertificateEntry createCertificateEntry() throws Exception {
        return createCertificateEntry(null, "common-name");
    }

    private CertificateEntry createCertificateEntry(String id, String commonName) throws Exception {
        return certificateService.builder().createRootCa(commonName, Algorithm.ED25519, Duration.ofDays(1)).withId(id);
    }
}
