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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.web.customization.CustomizationConfig;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.insertedIdsAsString;

/**
 * Service for managing certificate entries in MongoDB.
 * <p>
 * This service provides CRUD operations for storing and retrieving certificates
 * with their encrypted private keys. For certificate creation operations, see
 * {@link CertificateBuilder}. For PEM encoding/parsing, see {@link PemUtils}.
 */
@Singleton
public class CertificateService {

    private static final String COLLECTION_NAME = "pki_certificates";

    private final MongoCollection<CertificateEntry> collection;
    private final MongoUtils<CertificateEntry> utils;
    private final EncryptedValueService encryptedValueService;
    private final String productName;
    private final Clock clock;

    @Inject
    public CertificateService(MongoCollections mongoCollections,
                              EncryptedValueService encryptedValueService,
                              CustomizationConfig customizationConfig,
                              Clock clock) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, CertificateEntry.class);
        this.utils = mongoCollections.utils(collection);
        this.encryptedValueService = encryptedValueService;
        this.productName = customizationConfig.productName();
        this.clock = clock;

        collection.createIndex(
                Indexes.ascending(CertificateEntry.FIELD_FINGERPRINT),
                new IndexOptions().unique(true)
        );
        collection.createIndex(Indexes.ascending(CertificateEntry.FIELD_SUBJECT_KEY_IDENTIFIER));
    }

    /**
     * Returns the encrypted value service for encrypting/decrypting private keys.
     *
     * @return the encrypted value service
     */
    public EncryptedValueService encryptedValueService() {
        return encryptedValueService;
    }

    /**
     * Creates a new CertificateBuilder for creating certificates.
     * <p>
     * Example usage:
     * <pre>{@code
     * CertificateEntry rootCa = certificateService.builder()
     *     .createRootCa("My Root CA", Algorithm.ED25519, Duration.ofDays(3650));
     * rootCa = certificateService.insert(rootCa);
     * }</pre>
     *
     * @return a new CertificateBuilder instance
     */
    public CertificateBuilder builder() {
        return new CertificateBuilder(encryptedValueService, productName, clock);
    }

    /**
     * Inserts a new certificate entry into the database. If the given entity has a non-null ID, the method throws
     * an exception.
     *
     * @param entry the certificate entry to save
     * @return the saved certificate entry with its ID
     * @throws IllegalArgumentException when the given entry has a non-null ID
     */
    public CertificateEntry insert(CertificateEntry entry) {
        if (entry.id() == null) {
            return entry.withId(insertedIdAsString(collection.insertOne(entry)));
        } else {
            throw new IllegalArgumentException("new certificate entry should not have an ID");
        }
    }

    /**
     * Insert the given new certificate entries into the database. If any of the given entries has a non-null ID, the
     * method throws an exception.
     *
     * @param entries the new entries to insert
     * @return the inserted entries
     * @throws IllegalArgumentException when any of the given entries has a non-null ID
     */
    public List<CertificateEntry> insert(List<CertificateEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        if (entries.stream().anyMatch(entry -> entry.id() != null)) {
            throw new IllegalArgumentException("no entry should have an ID");
        }

        final var ids = insertedIdsAsString(collection.insertMany(entries));

        // Map the returned IDs to the given entries in order.
        return IntStream.range(0, entries.size())
                .mapToObj(idx -> entries.get(idx).withId(ids.get(idx)))
                .toList();
    }

    /**
     * Finds a certificate entry by its ID.
     *
     * @param id the ID of the certificate entry
     * @return an Optional containing the certificate entry if found, or empty if not found
     */
    public Optional<CertificateEntry> findById(String id) {
        return utils.getById(id);
    }

    /**
     * Finds a certificate entry by its fingerprint.
     *
     * @param fingerprint the fingerprint of the certificate
     * @return an Optional containing the certificate entry if found, or empty if not found
     */
    public Optional<CertificateEntry> findByFingerprint(String fingerprint) {
        return Optional.ofNullable(
                collection.find(Filters.eq(CertificateEntry.FIELD_FINGERPRINT, fingerprint)).first()
        );
    }

    /**
     * Finds a certificate entry by its Subject Key Identifier value.
     *
     * @param ski the Subject Key Identifier value
     * @return an Optional containing the certificate entry if found, or empty if not found
     */
    public Optional<CertificateEntry> findBySubjectKeyIdentifier(String ski) {
        return Optional.ofNullable(
                collection.find(Filters.eq(CertificateEntry.FIELD_SUBJECT_KEY_IDENTIFIER, ski)).first()
        );
    }

    /**
     * Returns all certificate entries in the collection.
     *
     * @return a list of all certificate entries
     */
    public List<CertificateEntry> findAll() {
        return collection.find().into(new ArrayList<>());
    }
}
