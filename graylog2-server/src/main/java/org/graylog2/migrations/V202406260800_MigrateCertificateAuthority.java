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
package org.graylog2.migrations;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.cluster.certificates.EncryptedCaKeystore;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class V202406260800_MigrateCertificateAuthority extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V202406260800_MigrateCertificateAuthority.class);
    public static final String LEGACY_COLLECTION_NAME = "graylog_ca_certificates";

    private final ClusterConfigService clusterConfigService;
    private final MongoConnection mongoConnection;

    @Inject
    public V202406260800_MigrateCertificateAuthority(final ClusterConfigService clusterConfigService, MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public MigrationType migrationType() {
        return MigrationType.PREFLIGHT;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-06-26T08:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V202406260800_MigrateCertificateAuthority.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        readExistingKeystore().ifPresent(keystore -> clusterConfigService.write(new EncryptedCaKeystore(keystore)));
        mongoConnection.getMongoDatabase().getCollection(LEGACY_COLLECTION_NAME).drop();
    }

    public Optional<EncryptedValue> readExistingKeystore() {
        MongoCollection<Document> dbCollection = mongoConnection.getMongoDatabase().getCollection(LEGACY_COLLECTION_NAME);
        final FindIterable<Document> objects = dbCollection.find(
                eq(
                        "node_id",
                        "GRAYLOG CA"
                )
        );
        final Document caKeystore = objects.first();

        if (caKeystore != null) {
            final Document encryptedCertificateDocument = caKeystore.get("encrypted_certificate_keystore", Document.class);
            if (encryptedCertificateDocument != null) {
                final EncryptedValue encryptedKeystore = EncryptedValue.builder()
                        .value(encryptedCertificateDocument.getString("encrypted_value"))
                        .salt(encryptedCertificateDocument.getString("salt"))
                        .isDeleteValue(false)
                        .isKeepValue(false)
                        .build();
                return Optional.ofNullable(encryptedKeystore);
            }
        }
        return Optional.empty();
    }

    public record MigrationCompleted() {
    }
}
