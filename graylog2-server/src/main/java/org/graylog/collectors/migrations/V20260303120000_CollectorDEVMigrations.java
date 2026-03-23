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
package org.graylog.collectors.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.KeyUtils;
import org.graylog.security.pki.PemUtils;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.streams.StreamRuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_ENROLLMENT_TOKEN_ID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_LAST_SEEN;

/**
 * Migration for Collector changes during the 7.1 development.
 * Must be removed before 7.1.
 */
public class V20260303120000_CollectorDEVMigrations extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260303120000_CollectorDEVMigrations.class);
    private static final String CLUSTER_CONFIG_COLLECTION = "cluster_config";
    private static final String INSTANCES_COLLECTION = "collector_instances";
    private static final String CERTIFICATES_COLLECTION = "pki_certificates";
    private static final String ENROLLMENT_TOKENS_COLLECTION = "collector_enrollment_tokens";
    private static final String INPUTS_COLLECTION = "inputs";
    private static final String CONFIG_TYPE = "org.graylog.collectors.CollectorsConfig";
    private static final String GRPC_INPUT_TYPE = "org.graylog.collectors.input.CollectorIngestGrpcInput";

    private final MongoConnection mongoConnection;
    private final EncryptedValueService encryptedValueService;

    @Inject
    public V20260303120000_CollectorDEVMigrations(MongoConnection mongoConnection, EncryptedValueService encryptedValueService) {
        this.mongoConnection = mongoConnection;
        this.encryptedValueService = encryptedValueService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-03-03T12:00:00Z");
    }

    @Override
    public void upgrade() {
        LOG.warn("This migration MUST be removed before the final 7.1 release!");

        final var db = mongoConnection.getMongoDatabase();

        // Order is important!
        convertObjectIdFields(db);
        renameCollections(db);
        renameCollectorsConfigFields(db);
        addCaCertIdToClusterConfig(db);
        updateStreamRule();
        backfillThresholdDefaults(db);
        convertLastSeenToBsonDate(db);
        backfillEnrollmentTokenId(db);
        deletePersistedGrpcInputs(db);
        removeGrpcConfig(db);
        deleteMacOSUnifiedLoggingSources(db);
        replaceTokenSigningCertWithSigningKey(db, encryptedValueService);
        addNamesToEnrollmentTokens(db);
        extendCollectorInstancesForCertRenewal(db);
    }

    private void convertObjectIdFields(MongoDatabase db) {
        final MongoCollection<Document> collection = db.getCollection("collector_instances");

        long converted = 0;
        converted += convertObjectIdFieldToString(collection, "fleet_id");
        converted += convertObjectIdFieldToString(collection, "issuing_ca_id");

        if (converted > 0) {
            LOG.info("Converted ObjectId fields to String in {} collector instance document(s)", converted);
        }
    }

    private long convertObjectIdFieldToString(MongoCollection<Document> collection, String fieldName) {
        final var cursor = collection.find(Filters.type(fieldName, "objectId"));

        long converted = 0;
        for (final Document doc : cursor) {
            final ObjectId objectId = doc.getObjectId(fieldName);
            collection.updateOne(
                    Filters.eq("_id", doc.getObjectId("_id")),
                    Updates.set(fieldName, objectId.toHexString())
            );
            converted++;
        }

        return converted;
    }

    private static void renameCollections(MongoDatabase db) {
        final var renamedCollections = Map.of(
                "fleets", "collector_fleets",
                "fleet_transaction_log", "collector_fleet_transaction_log",
                "fleet_sources", "collector_fleet_sources"
        );

        for (final var entry : renamedCollections.entrySet()) {
            final var fromColl = db.getCollection(entry.getKey());
            final var toColl = db.getCollection(entry.getValue());

            if (fromColl.countDocuments() > 0) {
                LOG.info("Migrating collection data from <{}> to <{}>", fromColl.getNamespace(), toColl.getNamespace());
                fromColl.find().forEach(doc -> {
                    LOG.info("  Document {}", doc.get("_id"));
                    toColl.insertOne(doc);
                    fromColl.deleteOne(Filters.eq("_id", doc.get("_id")));
                });

                if (fromColl.countDocuments() == 0) {
                    LOG.info("Dropping old collection <{}>", fromColl.getNamespace());
                    fromColl.drop();
                }
            }
        }
    }

    private static Optional<Document> loadCollectorsConfig(MongoDatabase db) {
        return Optional.ofNullable(db.getCollection("cluster_config")
                .find(Filters.eq("type", "org.graylog.collectors.CollectorsConfig"))
                .first());
    }

    private static void renameCollectorsConfigFields(MongoDatabase db) {
        final var oldName = "opamp_ca_id";
        final var newName = "signing_cert_id";
        final var payload = loadCollectorsConfig(db).map(c -> c.get("payload", Document.class));

        if (payload.isPresent() && isNotBlank(payload.get().getString(oldName))) {
            final var result = db.getCollection("cluster_config").updateOne(
                    Filters.eq("type", "org.graylog.collectors.CollectorsConfig"),
                    Updates.combine(
                            Updates.set("payload." + newName, payload.get().getString(oldName)),
                            Updates.unset("payload." + oldName)
                    )
            );
            if (result.getModifiedCount() > 0) {
                LOG.info("Renamed CollectorsConfig field <{}> to <{}>", oldName, newName);
            } else {
                LOG.warn("Couldn't rename CollectorsConfig field <{}> to <{}>", oldName, newName);
            }
        }
    }

    private static void addCaCertIdToClusterConfig(MongoDatabase db) {
        final var payload = loadCollectorsConfig(db).map(c -> c.get("payload", Document.class));
        final var newField = "ca_cert_id";

        if (payload.isPresent() && isBlank(payload.get().getString(newField))) {
            final var signingCertId = payload.get().getString("signing_cert_id");

            final var signingCert = db.getCollection(CERTIFICATES_COLLECTION)
                    .find(Filters.eq("_id", new ObjectId(signingCertId)))
                    .first();

            if (signingCert != null) {
                final var issuerCert = signingCert.getList("issuer_chain", String.class, List.of()).getFirst();

                final var caCert = db.getCollection("pki_certificates")
                        .find(Filters.eq("certificate", issuerCert))
                        .first();

                if (caCert != null) {
                    final var caCertId = caCert.getObjectId("_id").toHexString();
                    final var result = db.getCollection("cluster_config").updateOne(
                            Filters.eq("type", "org.graylog.collectors.CollectorsConfig"),
                            Updates.set("payload." + newField, caCertId)
                    );
                    if (result.getModifiedCount() > 0) {
                        LOG.info("Added {} field <{}> to CollectorsConfig cluster config value", newField, caCertId);
                    } else {
                        LOG.warn("Couldn't add {} field to CollectorsConfig cluster config value", newField);
                    }
                }
            }
        }
    }

    private void updateStreamRule() {
        // We renamed the field that contains the source type
        final var updateResult = mongoConnection.getMongoDatabase().getCollection("streamrules")
                .updateOne(
                        Filters.eq(StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(Stream.COLLECTOR_LOGS_STREAM_ID)),
                        Updates.set(StreamRuleImpl.FIELD_FIELD, CollectorIngestCodec.FIELD_COLLECTOR_SOURCE_TYPE)
                );
        if (updateResult.getModifiedCount() > 0) {
            LOG.info("Updated Collector stream rule to match on field <{}>", CollectorIngestCodec.FIELD_COLLECTOR_SOURCE_TYPE);
        }
    }

    private static void backfillThresholdDefaults(MongoDatabase db) {
        final MongoCollection<Document> collection = db.getCollection(CLUSTER_CONFIG_COLLECTION);
        final Document doc = collection.find(Filters.eq("type", CONFIG_TYPE)).first();
        if (doc == null) {
            LOG.debug("No collectors config found, skipping threshold backfill.");
            return;
        }

        final Document payload = doc.get("payload", Document.class);
        if (payload == null) {
            return;
        }

        final List<Bson> updates = new ArrayList<>();

        if (!payload.containsKey("collector_offline_threshold")) {
            updates.add(Updates.set("payload.collector_offline_threshold",
                    CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD.toString()));
        }

        if (!payload.containsKey("collector_default_visibility_threshold")) {
            updates.add(Updates.set("payload.collector_default_visibility_threshold",
                    CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD.toString()));
        }

        if (!payload.containsKey("collector_expiration_threshold")) {
            updates.add(Updates.set("payload.collector_expiration_threshold",
                    CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD.toString()));
        }

        if (!updates.isEmpty()) {
            collection.updateOne(Filters.eq("type", CONFIG_TYPE), Updates.combine(updates));
            LOG.info("Backfilled collectors config threshold defaults.");
        }
    }

    private static void convertLastSeenToBsonDate(MongoDatabase db) {
        final MongoCollection<Document> collection = db.getCollection(INSTANCES_COLLECTION);

        long converted = 0;

        for (final Document doc : collection.find(Filters.type(FIELD_LAST_SEEN, "string"))) {
            final String value = doc.getString(FIELD_LAST_SEEN);
            final Date date = Date.from(Instant.parse(value));
            collection.updateOne(
                    Filters.eq("_id", doc.getObjectId("_id")),
                    Updates.set(FIELD_LAST_SEEN, date)
            );
            converted++;
        }

        if (converted > 0) {
            LOG.info("Converted last_seen to BSON Date in {} collector instance document(s).", converted);
        }
    }

    private static void backfillEnrollmentTokenId(MongoDatabase db) {
        final MongoCollection<Document> collection = db.getCollection(INSTANCES_COLLECTION);

        final long updated = collection.updateMany(
                Filters.not(Filters.exists(FIELD_ENROLLMENT_TOKEN_ID)),
                Updates.set(FIELD_ENROLLMENT_TOKEN_ID, "000000000000000000000000")
        ).getModifiedCount();

        if (updated > 0) {
            LOG.info("Backfilled enrollment_token_id in {} collector instance document(s).", updated);
        }
    }

    private static void deletePersistedGrpcInputs(MongoDatabase db) {
        final MongoCollection<Document> collection = db.getCollection(INPUTS_COLLECTION);

        final long deleted = collection.deleteMany(Filters.eq("type", GRPC_INPUT_TYPE)).getDeletedCount();
        if (deleted > 0) {
            LOG.info("Deleted {} persisted collector gRPC input(s).", deleted);
        }
    }

    private static void deleteMacOSUnifiedLoggingSources(MongoDatabase db) {
        final MongoCollection<Document> collection = db.getCollection("collector_fleet_sources");

        final long deleted = collection.deleteMany(Filters.eq("config.type", "macos_unified_logging")).getDeletedCount();
        if (deleted > 0) {
            LOG.info("Deleted {} macOS unified logging source(s).", deleted);
        }
    }

    private static void removeGrpcConfig(MongoDatabase db) {
        final MongoCollection<Document> collection = db.getCollection(CLUSTER_CONFIG_COLLECTION);
        final Document doc = collection.find(Filters.eq("type", CONFIG_TYPE)).first();
        if (doc == null) {
            return;
        }

        final Document payload = doc.get("payload", Document.class);
        if (payload == null || !payload.containsKey("grpc")) {
            return;
        }

        final long updated = collection.updateOne(
                Filters.eq("type", CONFIG_TYPE),
                Updates.unset("payload.grpc")
        ).getModifiedCount();
        if (updated > 0) {
            LOG.info("Removed gRPC settings from collectors config.");
        }
    }

    private static void replaceTokenSigningCertWithSigningKey(MongoDatabase db, EncryptedValueService encryptedValueService) {
        final var oldField = "token_signing_cert_id";
        final var newField = "token_signing_key";

        final var clusterConfigCollection = db.getCollection(CLUSTER_CONFIG_COLLECTION);
        final Document config = clusterConfigCollection.find(Filters.eq("type", CONFIG_TYPE)).first();
        if (config == null) {
            return;
        }

        final Document payload = config.get("payload", Document.class);

        if (payload != null && payload.containsKey(oldField)) {
            final var signingCertId = payload.getString(oldField);
            final var certificatesCollection = db.getCollection(CERTIFICATES_COLLECTION);

            final var certDeleteResult = certificatesCollection.deleteOne(Filters.eq("_id", new ObjectId(signingCertId)));
            if (certDeleteResult.getDeletedCount() != 1) {
                LOG.warn("Could not delete certificate with signing cert id {}", signingCertId);
            }
            final var result = clusterConfigCollection.updateOne(
                    Filters.eq("type", CONFIG_TYPE),
                    Updates.unset("payload." + oldField)
            );
            if (result.getModifiedCount() != 1) {
                LOG.warn("Could not update Collectors cluster config to remove field: {}", oldField);
            }
        }

        if (payload == null || payload.containsKey(newField)) {
            return;
        }

        try {
            final var keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);

            final var tsk = new TokenSigningKey(
                    encryptedValueService.encrypt(PemUtils.toPem(keyPair.getPrivate())),
                    PemUtils.toPem(keyPair.getPublic()),
                    KeyUtils.sha256Fingerprint(keyPair),
                    Instant.now()
            );

            final var tskDoc = new Document();

            tskDoc.put("private_key", new Document(Map.of(
                    "encrypted_value", tsk.privateKey().value(),
                    "salt", tsk.privateKey().salt()
            )));
            tskDoc.put("public_key", tsk.publicKey());
            tskDoc.put("fingerprint", tsk.fingerprint());
            tskDoc.put("created_at", Date.from(Instant.now()));

            final var result = clusterConfigCollection.updateOne(
                    Filters.eq("type", CONFIG_TYPE),
                    Updates.set("payload." + newField, tskDoc)
            );
            if (result.getModifiedCount() != 1) {
                LOG.warn("Could not update Collectors cluster config to add new field: {}", newField);
            }

            db.getCollection(ENROLLMENT_TOKENS_COLLECTION).deleteMany(Filters.empty());
        } catch (Exception e) {
            LOG.error("Could not generate token signing key.", e);
        }
    }

    private void addNamesToEnrollmentTokens(MongoDatabase db) {
        final var collection = db.getCollection(ENROLLMENT_TOKENS_COLLECTION);
        collection.updateMany(
                Filters.not(Filters.exists(EnrollmentTokenDTO.FIELD_NAME)),
                Updates.set(EnrollmentTokenDTO.FIELD_NAME, "Unnamed token")
        );
    }

    private static void extendCollectorInstancesForCertRenewal(MongoDatabase db) {
        final var collection = db.getCollection(INSTANCES_COLLECTION);

        final var enrolledAtField = "enrolled_at";
        final var certificatePemField = "certificate_pem";
        final var certificateFingerprintField = "certificate_fingerprint";

        final var activeCertificatePemField = "active_certificate_pem";
        final var activeCertificateFingerprintField = "active_certificate_fingerprint";
        final var activeCertificateExpiresAt = "active_certificate_expires_at";

        // Index must be deleted first!
        for (final var index : collection.listIndexes()) {
            if (index.get("key", Document.class).containsKey(certificateFingerprintField)) {
                collection.dropIndex(index.getString("name"));
            }
        }

        var converted = 0L;

        try {
            for (final var doc : collection.find(Filters.exists(certificatePemField))) {
                final var enrolledAt = doc.getString(enrolledAtField);
                final var activeCertificatePem = doc.getString(certificatePemField);
                final var activeCertificateFingerprint = doc.getString(certificateFingerprintField);
                final var cert = PemUtils.parseCertificate(activeCertificatePem);

                collection.updateOne(
                        Filters.eq("_id", doc.getObjectId("_id")),
                        Updates.combine(
                                Updates.unset(certificatePemField),
                                Updates.unset(certificateFingerprintField),
                                Updates.set(activeCertificatePemField, activeCertificatePem),
                                Updates.set(activeCertificateFingerprintField, activeCertificateFingerprint),
                                Updates.set(activeCertificateExpiresAt, cert.getNotAfter()),
                                Updates.set(enrolledAtField, Date.from(Instant.parse(enrolledAt)))
                        )
                );
                converted++;
            }
        } catch (CertificateException | IOException e) {
            throw new RuntimeException(e);
        }

        if (converted > 0) {
            LOG.info("Converted {} collector instance document(s) to new certificate fields.", converted);
        }
    }
}
