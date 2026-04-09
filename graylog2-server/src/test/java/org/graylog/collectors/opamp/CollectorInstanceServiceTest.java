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
package org.graylog.collectors.opamp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.grn.GRNRegistry;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CollectorInstanceService}.
 */
@ExtendWith(MongoDBExtension.class)
class CollectorInstanceServiceTest {

    private CollectorInstanceService collectorInstanceService;
    private MongoCollections mongoCollections;

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

        mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper),
                mongodb.mongoConnection()
        );
        collectorInstanceService = new CollectorInstanceService(mongoCollections);
    }

    @Test
    void enrollAssignsIdToNewCollector() {
        final CollectorInstanceDTO collector = enroll(collectorInstanceService, "instance-uid-1", "sha256:fingerprint1");

        assertThat(collector.id()).isNotNull();
        assertThat(collector.instanceUid()).isEqualTo("instance-uid-1");
        assertThat(collector.activeCertificateFingerprint()).isEqualTo("sha256:fingerprint1");
        assertThat(collector.activeCertificateExpiresAt()).isEqualTo(Instant.ofEpochMilli(0).plus(Duration.ofDays(7)));
    }

    @Test
    void findByInstanceUidReturnsCollector() {
        enroll(collectorInstanceService, "instance-uid-2", "sha256:fingerprint2");

        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByInstanceUid("instance-uid-2");

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("instance-uid-2");
        assertThat(found.get().activeCertificateFingerprint()).isEqualTo("sha256:fingerprint2");
        assertThat(found.get().activeCertificateExpiresAt()).isEqualTo(Instant.ofEpochMilli(0).plus(Duration.ofDays(7)));
    }

    @Test
    void findByInstanceUidReturnsEmptyForUnknown() {
        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByInstanceUid("non-existent-uid");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByInstanceUidReturnsTrueForExisting() {
        enroll(collectorInstanceService, "instance-uid-4", "sha256:fingerprint4");

        final boolean exists = collectorInstanceService.existsByInstanceUid("instance-uid-4");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByInstanceUidReturnsFalseForUnknown() {
        final boolean exists = collectorInstanceService.existsByInstanceUid("non-existent-uid");

        assertThat(exists).isFalse();
    }

    @Test
    void countByFleetGroupedReturnsPerFleetCounts() {
        final Instant now = Instant.now();
        final Instant recentlySeen = now.minusSeconds(30);
        final Instant longAgo = now.minusSeconds(600);
        final Instant onlineThreshold = now.minusSeconds(60);

        final String fleetA = "507f1f77bcf86cd799439012";
        final String fleetB = "507f1f77bcf86cd799439013";

        // fleet-a: 3 instances (2 online, 1 offline based on threshold)
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-a1", "sha256:fp-a1", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-a2", "sha256:fp-a2", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-a3", "sha256:fp-a3", fleetA, longAgo);

        // fleet-b: 1 instance (1 online)
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-b1", "sha256:fp-b1", fleetB, recentlySeen);

        final Map<String, long[]> grouped = collectorInstanceService.countByFleetGrouped(onlineThreshold);

        assertThat(grouped).containsKey(fleetA);
        assertThat(grouped.get(fleetA)[0]).isEqualTo(3L); // total
        assertThat(grouped.get(fleetA)[1]).isEqualTo(2L); // online

        assertThat(grouped).containsKey(fleetB);
        assertThat(grouped.get(fleetB)[0]).isEqualTo(1L); // total
        assertThat(grouped.get(fleetB)[1]).isEqualTo(1L); // online

        // absent fleet should not be in the map
        assertThat(grouped).doesNotContainKey("507f1f77bcf86cd799439099");
    }

    @Test
    void findByInstanceUidsReturnsMappedResults() {
        enroll(collectorInstanceService, "uid-1", "sha256:fp-1");
        enroll(collectorInstanceService, "uid-2", "sha256:fp-2");
        enroll(collectorInstanceService, "uid-3", "sha256:fp-3");

        Map<String, CollectorInstanceDTO> result = collectorInstanceService.findByInstanceUids(Set.of("uid-1", "uid-3"));

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("uid-1", "uid-3");
        assertThat(result.get("uid-1").instanceUid()).isEqualTo("uid-1");
    }

    @Test
    void findByInstanceUidsReturnsEmptyForNoMatches() {
        Map<String, CollectorInstanceDTO> result = collectorInstanceService.findByInstanceUids(Set.of("nonexistent"));

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByInstanceUidDeletesExistingInstance() {
        enroll(collectorInstanceService, "uid-to-delete", "sha256:fp-delete");

        final boolean deleted = collectorInstanceService.deleteByInstanceUid("uid-to-delete");

        assertThat(deleted).isTrue();
        assertThat(collectorInstanceService.findByInstanceUid("uid-to-delete")).isEmpty();
    }

    @Test
    void deleteByInstanceUidReturnsFalseForNonExistent() {
        final boolean deleted = collectorInstanceService.deleteByInstanceUid("non-existent");

        assertThat(deleted).isFalse();
    }

    @Test
    void deleteExpiredRemovesOldInstances() {
        final Instant now = Instant.now();
        final Duration threshold = Duration.ofDays(7);

        // Expired: last seen 8 days ago
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-expired",
                "sha256:fp-expired", "507f1f77bcf86cd799439012", now.minus(Duration.ofDays(8)));
        // Not expired: last seen 3 days ago
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-recent",
                "sha256:fp-recent", "507f1f77bcf86cd799439012", now.minus(Duration.ofDays(3)));

        final long deleted = collectorInstanceService.deleteExpired(threshold);

        assertThat(deleted).isEqualTo(1);
        assertThat(collectorInstanceService.findByInstanceUid("uid-expired")).isEmpty();
        assertThat(collectorInstanceService.findByInstanceUid("uid-recent")).isPresent();
    }

    @Test
    void deleteExpiredReturnsZeroWhenNothingToDelete() {
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-fresh",
                "sha256:fp-fresh", "507f1f77bcf86cd799439012", Instant.now());

        final long deleted = collectorInstanceService.deleteExpired(Duration.ofDays(7));

        assertThat(deleted).isEqualTo(0);
    }

    @Test
    void findByActiveOrNextFingerprintMatchesActiveFingerprint() {
        enroll(collectorInstanceService, "uid-active", "sha256:active-fp");

        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByActiveOrNextFingerprint("sha256:active-fp");

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("uid-active");
    }

    @Test
    void findByActiveOrNextFingerprintMatchesNextFingerprint() {
        enroll(collectorInstanceService, "uid-next", "sha256:active-fp-2");
        setNextCertificateFields("uid-next", "sha256:next-fp", "next-cert-pem",
                Instant.now().plus(Duration.ofDays(30)));

        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByActiveOrNextFingerprint("sha256:next-fp");

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("uid-next");
        assertThat(found.get().nextCertificateFingerprint()).hasValue("sha256:next-fp");
    }

    @Test
    void findByActiveOrNextFingerprintReturnsEmptyForUnknown() {
        enroll(collectorInstanceService, "uid-no-match", "sha256:some-fp");

        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByActiveOrNextFingerprint("sha256:unknown-fp");

        assertThat(found).isEmpty();
    }

    @Test
    void activateNextCertificatePromotesNextToActive() {
        enroll(collectorInstanceService, "uid-activate", "sha256:old-active-fp");
        final var nextExpiresAt = Instant.now().plus(Duration.ofDays(30));
        setNextCertificateFields("uid-activate", "sha256:new-fp", "new-cert-pem", nextExpiresAt);

        // Read back the instance with next certificate fields set
        final var withNext = collectorInstanceService.findByInstanceUid("uid-activate").orElseThrow();
        assertThat(collectorInstanceService.activateNextCertificate(withNext)).isTrue();

        // Verify the promoted active_certificate_expires_at is a BSON Date, not a String
        assertFieldIsDate("uid-activate", CollectorInstanceDTO.FIELD_ACTIVE_CERTIFICATE_EXPIRES_AT);

        // Verify that next certificate fields are now active
        final var updated = collectorInstanceService.findByInstanceUid("uid-activate").orElseThrow();
        assertThat(updated.activeCertificateFingerprint()).isEqualTo("sha256:new-fp");
        assertThat(updated.activeCertificatePem()).isEqualTo("new-cert-pem");
        assertThat(updated.activeCertificateExpiresAt()).isEqualTo(Date.from(nextExpiresAt).toInstant());

        // Next certificate fields should be cleared
        assertThat(updated.nextCertificateFingerprint()).isEmpty();
        assertThat(updated.nextCertificatePem()).isEmpty();
        assertThat(updated.nextCertificateExpiresAt()).isEmpty();
    }

    @Test
    void activateNextCertificateThrowsWhenNextFieldsMissing() {
        final CollectorInstanceDTO enrolled = enroll(collectorInstanceService, "uid-no-next", "sha256:fp-no-next");

        assertThatThrownBy(() -> collectorInstanceService.activateNextCertificate(enrolled))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void insertNextCertificateSetsNextFields() {
        enroll(collectorInstanceService, "uid-insert-next", "sha256:active-fp");
        final Instant nextExpiresAt = Instant.now().plus(Duration.ofDays(30));

        final boolean result = collectorInstanceService.insertNextCertificate(
                "uid-insert-next", "sha256:next-fp", "next-cert-pem", nextExpiresAt);

        // Verify BSON type is Date, not String
        assertFieldIsDate("uid-insert-next", CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_EXPIRES_AT);

        assertThat(result).isTrue();
        final var updated = collectorInstanceService.findByInstanceUid("uid-insert-next").orElseThrow();
        assertThat(updated.nextCertificateFingerprint()).hasValue("sha256:next-fp");
        assertThat(updated.nextCertificatePem()).hasValue("next-cert-pem");
        assertThat(updated.nextCertificateExpiresAt()).hasValue(Date.from(nextExpiresAt).toInstant());
        // Active certificate should remain unchanged
        assertThat(updated.activeCertificateFingerprint()).isEqualTo("sha256:active-fp");
    }

    @Test
    void insertNextCertificateReturnsFalseForNonExistentInstance() {
        final boolean result = collectorInstanceService.insertNextCertificate(
                "non-existent-uid", "sha256:next-fp", "next-cert-pem", Instant.now().plus(Duration.ofDays(30)));

        assertThat(result).isFalse();
    }

    @Test
    void insertNextCertificateOverwritesPreviousNextFields() {
        enroll(collectorInstanceService, "uid-overwrite-next", "sha256:active-fp");
        final Instant firstExpiresAt = Instant.now().plus(Duration.ofDays(10));
        final Instant secondExpiresAt = Instant.now().plus(Duration.ofDays(20));

        collectorInstanceService.insertNextCertificate(
                "uid-overwrite-next", "sha256:first-next-fp", "first-pem", firstExpiresAt);
        collectorInstanceService.insertNextCertificate(
                "uid-overwrite-next", "sha256:second-next-fp", "second-pem", secondExpiresAt);

        // Verify BSON type is Date, not String
        assertFieldIsDate("uid-overwrite-next", CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_EXPIRES_AT);

        final var updated = collectorInstanceService.findByInstanceUid("uid-overwrite-next").orElseThrow();
        assertThat(updated.nextCertificateFingerprint()).hasValue("sha256:second-next-fp");
        assertThat(updated.nextCertificatePem()).hasValue("second-pem");
        assertThat(updated.nextCertificateExpiresAt()).hasValue(Date.from(secondExpiresAt).toInstant());
    }

    private static CollectorInstanceDTO enroll(CollectorInstanceService service, String instanceUid, String fingerprint) {
        return service.enroll(
                instanceUid,
                "507f1f77bcf86cd799439012", // Valid 24-char hex ObjectId
                fingerprint,
                "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
                Date.from(Instant.ofEpochMilli(0).plus(Duration.ofDays(7))),
                "507f1f77bcf86cd799439011", // Valid 24-char hex ObjectId
                Instant.now(),
                "000000000000000000000000"
        );
    }

    private static CollectorInstanceDTO enrollWithFleetAndLastSeen(CollectorInstanceService service,
                                                                   String instanceUid, String fingerprint,
                                                                   String fleetId, Instant lastSeen) {
        return service.enroll(
                instanceUid,
                fleetId,
                fingerprint,
                "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
                Date.from(Instant.ofEpochMilli(0).plus(Duration.ofDays(7))),
                "507f1f77bcf86cd799439011",
                lastSeen,
                "000000000000000000000000"
        );
    }

    private Optional<Document> findRawDocument(String instanceUid) {
        return Optional.ofNullable(mongoCollections.nonEntityCollection("collector_instances", Document.class)
                .find(Filters.eq(CollectorInstanceDTO.FIELD_INSTANCE_UID, instanceUid))
                .first());
    }

    private void assertFieldIsDate(String instanceUid, String fieldName) {
        assertThat(findRawDocument(instanceUid).orElseThrow().get(fieldName))
                .isInstanceOf(Date.class);
    }

    private void setNextCertificateFields(String instanceUid, String fingerprint, String pem, Instant expiresAt) {
        final var collection = mongoCollections.collection("collector_instances", CollectorInstanceDTO.class);
        collection.updateOne(
                Filters.eq(CollectorInstanceDTO.FIELD_INSTANCE_UID, instanceUid),
                combine(
                        set(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_FINGERPRINT, fingerprint),
                        set(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_PEM, pem),
                        set(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_EXPIRES_AT, expiresAt)
                )
        );
    }
}
