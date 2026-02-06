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
import com.mongodb.client.model.ReplaceOptions;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.security.encryption.EncryptedValueService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

/**
 * Service for managing certificate entries in MongoDB.
 * <p>
 * This service provides CRUD operations for storing and retrieving certificates
 * with their encrypted private keys. For certificate creation operations, see
 * {@link CertificateBuilder}. For PEM encoding/parsing, see {@link PemUtils}.
 */
@Singleton
public class CertificateService {

    private static final String COLLECTION_NAME = "certificates";

    private final MongoCollection<CertificateEntry> collection;
    private final MongoUtils<CertificateEntry> utils;
    private final EncryptedValueService encryptedValueService;

    @Inject
    public CertificateService(MongoCollections mongoCollections, EncryptedValueService encryptedValueService) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, CertificateEntry.class);
        this.utils = mongoCollections.utils(collection);
        this.encryptedValueService = encryptedValueService;

        collection.createIndex(
                Indexes.ascending(CertificateEntry.FIELD_FINGERPRINT),
                new IndexOptions().unique(true)
        );
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
     * rootCa = certificateService.save(rootCa);
     * }</pre>
     *
     * @return a new CertificateBuilder instance
     */
    public CertificateBuilder builder() {
        return new CertificateBuilder(encryptedValueService);
    }

    /**
     * Saves a certificate entry to the database.
     * If the entry has a null ID, it will be inserted as a new document.
     * If the entry has an existing ID, it will replace the existing document.
     *
     * @param entry the certificate entry to save
     * @return the saved certificate entry with its ID
     */
    public CertificateEntry save(CertificateEntry entry) {
        if (entry.id() == null) {
            final String insertedId = insertedIdAsString(collection.insertOne(entry));
            return entry.withId(insertedId);
        } else {
            collection.replaceOne(
                    Filters.eq("_id", new ObjectId(entry.id())),
                    entry,
                    new ReplaceOptions().upsert(false)
            );
            return entry;
        }
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
     * Returns all certificate entries in the collection.
     *
     * @return a list of all certificate entries
     */
    public List<CertificateEntry> findAll() {
        return collection.find().into(new ArrayList<>());
    }
}
