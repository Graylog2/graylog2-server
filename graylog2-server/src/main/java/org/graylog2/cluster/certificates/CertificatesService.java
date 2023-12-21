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
package org.graylog2.cluster.certificates;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoCollection;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoCollections;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.database.MongoConnection;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;

import jakarta.inject.Inject;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;


public class CertificatesService {

    private static final String ENCRYPTED_VALUE_SUBFIELD = "encrypted_value";
    private static final String SALT_SUBFIELD = "salt";

    private final MongoDatabase mongoDatabase;
    private final EncryptedValueService encryptionService;

    @Inject
    public CertificatesService(final MongoConnection mongoConnection,
                               final EncryptedValueService encryptionService) {

        this.mongoDatabase = mongoConnection.getMongoDatabase();
        this.encryptionService = encryptionService;
        KeystoreMongoCollections.ALL_KEYSTORE_COLLECTIONS.forEach(collection -> {
            final MongoCollection<Document> dbCollection = mongoDatabase
                    .getCollection(collection.collectionName());
            dbCollection.createIndex(
                    ascending(collection.identifierField()),
                    new IndexOptions().unique(true));
        });


    }

    public boolean writeCert(final KeystoreMongoLocation keystoreMongoLocation,
                             final String cert) {
        final KeystoreMongoCollection collection = keystoreMongoLocation.collection();
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(collection.collectionName());
        final EncryptedValue encrypted = encryptionService.encrypt(cert);
        final UpdateResult result = dbCollection.updateOne(
                eq(collection.identifierField(), keystoreMongoLocation.nodeId()),
                combine(
                        set(collection.identifierField(), keystoreMongoLocation.nodeId()),
                        set(collection.encryptedCertificateField() + "." + ENCRYPTED_VALUE_SUBFIELD, encrypted.value()),
                        set(collection.encryptedCertificateField() + "." + SALT_SUBFIELD, encrypted.salt())
                ),
                new UpdateOptions().upsert(true)
        );
        return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
    }

    public boolean hasCert(final KeystoreMongoLocation keystoreMongoLocation) {
        final KeystoreMongoCollection collection = keystoreMongoLocation.collection();
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(collection.collectionName());
        final FindIterable<Document> objects = dbCollection.find(
                eq(
                        collection.identifierField(),
                        keystoreMongoLocation.nodeId()
                )
        );
        final Document nodeCertificate = objects.first();
        if (nodeCertificate != null) {
            final Document encryptedCertificateDocument = nodeCertificate.get(collection.encryptedCertificateField(), Document.class);
            if (encryptedCertificateDocument != null) {
                return true;
            }
        }
        return false;
    }

    public Optional<String> readCert(final KeystoreMongoLocation keystoreMongoLocation) {
        final KeystoreMongoCollection collection = keystoreMongoLocation.collection();
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(collection.collectionName());
        final FindIterable<Document> objects = dbCollection.find(
                eq(
                        collection.identifierField(),
                        keystoreMongoLocation.nodeId()
                )
        );
        final Document nodeCertificate = objects.first();

        if (nodeCertificate != null) {
            final Document encryptedCertificateDocument = nodeCertificate.get(collection.encryptedCertificateField(), Document.class);
            if (encryptedCertificateDocument != null) {
                final EncryptedValue encryptedCertificate = EncryptedValue.builder()
                        .value(encryptedCertificateDocument.getString(ENCRYPTED_VALUE_SUBFIELD))
                        .salt(encryptedCertificateDocument.getString(SALT_SUBFIELD))
                        .isDeleteValue(false)
                        .isKeepValue(false)
                        .build();

                return Optional.ofNullable(encryptionService.decrypt(encryptedCertificate));
            }
        }
        return Optional.empty();
    }

    public boolean removeCert(final KeystoreMongoLocation keystoreMongoLocation) {
        final KeystoreMongoCollection collection = keystoreMongoLocation.collection();
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(collection.collectionName());
        var result = dbCollection.deleteOne(
                eq(
                        collection.identifierField(),
                        keystoreMongoLocation.nodeId()
                )
        );
        return result.getDeletedCount() > 0;
    }
}
