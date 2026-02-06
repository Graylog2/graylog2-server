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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.grn.GRNRegistry;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

@ExtendWith(MongoDBExtension.class)
class CertificateEntryTest {

    private ObjectMapper objectMapper;
    private EncryptedValueService encryptedValueService;
    private MongoCollection<CertificateEntry> collection;
    private MongoUtils<CertificateEntry> utils;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        objectMapper = new ObjectMapperProvider(
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
        collection = mongoCollections.collection("certificate_entries", CertificateEntry.class);
        utils = mongoCollections.utils(collection);
    }

    @Test
    void implementsMongoEntity() {
        assertThat(MongoEntity.class).isAssignableFrom(CertificateEntry.class);
    }

    @Test
    void fieldConstantsAreDefined() {
        assertThat(CertificateEntry.FIELD_ID).isEqualTo("id");
        assertThat(CertificateEntry.FIELD_FINGERPRINT).isEqualTo("fingerprint");
        assertThat(CertificateEntry.FIELD_PRIVATE_KEY).isEqualTo("private_key");
        assertThat(CertificateEntry.FIELD_CERTIFICATE).isEqualTo("certificate");
        assertThat(CertificateEntry.FIELD_ISSUER_CHAIN).isEqualTo("issuer_chain");
        assertThat(CertificateEntry.FIELD_NOT_BEFORE).isEqualTo("not_before");
        assertThat(CertificateEntry.FIELD_NOT_AFTER).isEqualTo("not_after");
        assertThat(CertificateEntry.FIELD_CREATED_AT).isEqualTo("created_at");
    }

    @Test
    void recordFieldsAreAccessible() {
        final EncryptedValue privateKey = createEncryptedValue();
        final Instant notBefore = Instant.parse("2024-01-01T00:00:00Z");
        final Instant notAfter = Instant.parse("2025-01-01T00:00:00Z");
        final Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        final List<String> issuerChain = List.of("-----BEGIN CERTIFICATE-----\nISSUER\n-----END CERTIFICATE-----");

        final CertificateEntry entry = new CertificateEntry(
                "test-id",
                "SHA256:abc123",
                privateKey,
                "-----BEGIN CERTIFICATE-----\nTEST\n-----END CERTIFICATE-----",
                issuerChain,
                notBefore,
                notAfter,
                createdAt
        );

        assertThat(entry.id()).isEqualTo("test-id");
        assertThat(entry.fingerprint()).isEqualTo("SHA256:abc123");
        assertThat(entry.privateKey()).isEqualTo(privateKey);
        assertThat(entry.certificate()).isEqualTo("-----BEGIN CERTIFICATE-----\nTEST\n-----END CERTIFICATE-----");
        assertThat(entry.issuerChain()).isEqualTo(issuerChain);
        assertThat(entry.notBefore()).isEqualTo(notBefore);
        assertThat(entry.notAfter()).isEqualTo(notAfter);
        assertThat(entry.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void withIdCreatesNewInstanceWithNewId() {
        final EncryptedValue privateKey = createEncryptedValue();
        final Instant notBefore = Instant.parse("2024-01-01T00:00:00Z");
        final Instant notAfter = Instant.parse("2025-01-01T00:00:00Z");
        final Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        final CertificateEntry original = new CertificateEntry(
                null,
                "SHA256:abc123",
                privateKey,
                "-----BEGIN CERTIFICATE-----\nTEST\n-----END CERTIFICATE-----",
                List.of(),
                notBefore,
                notAfter,
                createdAt
        );

        final CertificateEntry withNewId = original.withId("new-id");

        assertThat(withNewId.id()).isEqualTo("new-id");
        assertThat(withNewId.fingerprint()).isEqualTo(original.fingerprint());
        assertThat(withNewId.privateKey()).isEqualTo(original.privateKey());
        assertThat(withNewId.certificate()).isEqualTo(original.certificate());
        assertThat(withNewId.issuerChain()).isEqualTo(original.issuerChain());
        assertThat(withNewId.notBefore()).isEqualTo(original.notBefore());
        assertThat(withNewId.notAfter()).isEqualTo(original.notAfter());
        assertThat(withNewId.createdAt()).isEqualTo(original.createdAt());
    }

    @Test
    void jsonSerializationUsesCorrectPropertyNames() throws Exception {
        final EncryptedValue privateKey = createEncryptedValue();
        final Instant notBefore = Instant.parse("2024-01-01T00:00:00Z");
        final Instant notAfter = Instant.parse("2025-01-01T00:00:00Z");
        final Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        final CertificateEntry entry = new CertificateEntry(
                "test-id",
                "SHA256:abc123",
                privateKey,
                "-----BEGIN CERTIFICATE-----\nTEST\n-----END CERTIFICATE-----",
                List.of("-----BEGIN CERTIFICATE-----\nISSUER\n-----END CERTIFICATE-----"),
                notBefore,
                notAfter,
                createdAt
        );

        final String json = objectMapper.writeValueAsString(entry);
        final JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("id")).isTrue();
        assertThat(node.has("fingerprint")).isTrue();
        assertThat(node.has("private_key")).isTrue();
        assertThat(node.has("certificate")).isTrue();
        assertThat(node.has("issuer_chain")).isTrue();
        assertThat(node.has("not_before")).isTrue();
        assertThat(node.has("not_after")).isTrue();
        assertThat(node.has("created_at")).isTrue();
    }

    @Test
    void mongoDbRoundTrip() {
        final EncryptedValue privateKey = createEncryptedValue();
        final Instant notBefore = Instant.parse("2024-01-01T00:00:00Z");
        final Instant notAfter = Instant.parse("2025-01-01T00:00:00Z");
        final Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        final CertificateEntry entry = new CertificateEntry(
                null,
                "SHA256:abc123",
                privateKey,
                "-----BEGIN CERTIFICATE-----\nTEST\n-----END CERTIFICATE-----",
                List.of("-----BEGIN CERTIFICATE-----\nISSUER\n-----END CERTIFICATE-----"),
                notBefore,
                notAfter,
                createdAt
        );

        final String savedId = insertedIdAsString(collection.insertOne(entry));
        final Optional<CertificateEntry> retrieved = utils.getById(savedId);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().id()).isEqualTo(savedId);
        assertThat(retrieved.get().fingerprint()).isEqualTo("SHA256:abc123");
        assertThat(retrieved.get().privateKey()).isEqualTo(privateKey);
        assertThat(retrieved.get().certificate()).isEqualTo("-----BEGIN CERTIFICATE-----\nTEST\n-----END CERTIFICATE-----");
        assertThat(retrieved.get().issuerChain()).containsExactly("-----BEGIN CERTIFICATE-----\nISSUER\n-----END CERTIFICATE-----");
        assertThat(retrieved.get().notBefore()).isEqualTo(notBefore);
        assertThat(retrieved.get().notAfter()).isEqualTo(notAfter);
        assertThat(retrieved.get().createdAt()).isEqualTo(createdAt);
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
