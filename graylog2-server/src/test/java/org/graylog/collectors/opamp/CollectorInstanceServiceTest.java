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

import com.mongodb.client.model.Filters;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bson.Document;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorOSType;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.testing.TestClocks;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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
    private static final EncryptedValueService encryptedValueService = new EncryptedValueService("abcdef1234567890");
    private static CertificateBuilder certBuilder;
    private static CertificateEntry issuerCert;

    private CollectorInstanceService collectorInstanceService;
    private MongoCollections mongoCollections;

    @BeforeAll
    static void beforeAll() throws Exception {
        certBuilder = new CertificateBuilder(encryptedValueService, "Graylog", TestClocks.fixedEpoch());
        final CertificateEntry caCert = certBuilder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(7)).withId("100000000000000000000000");
        issuerCert = certBuilder.createIntermediateCa("Test Issuer", caCert, Duration.ofDays(5)).withId("200000000000000000000000");
    }

    @BeforeEach
    void setUp(MongoCollections coll) {
        mongoCollections = coll;
        collectorInstanceService = new CollectorInstanceService(coll);
    }

    @Test
    void enrollAssignsIdToNewCollector() throws Exception {
        final var collector = enroll("instance-uid-1");

        assertThat(collector.id()).isNotNull();
        assertThat(collector.instanceUid()).isEqualTo("instance-uid-1");
        assertThat(collector.activeCertificateFingerprint()).matches("sha256:[a-f0-9]{64}");
        assertThat(collector.activeCertificateExpiresAt()).isEqualTo(Instant.ofEpochMilli(0).plus(Duration.ofDays(1)));
    }

    @Test
    void findByInstanceUidReturnsCollector() throws Exception {
        final var instance = enroll("instance-uid-1");

        assertThat(collectorInstanceService.findByInstanceUid("instance-uid-1")).hasValueSatisfying(found -> {
            assertThat(found.instanceUid()).isEqualTo("instance-uid-1");
            assertThat(found.activeCertificateFingerprint()).isEqualTo(instance.activeCertificateFingerprint());
        });
    }

    @Test
    void findByInstanceUidReturnsEmptyForUnknown() {
        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByInstanceUid("non-existent-uid");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByInstanceUidReturnsTrueForExisting() throws Exception {
        enroll("instance-uid-1");

        final boolean exists = collectorInstanceService.existsByInstanceUid("instance-uid-1");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByInstanceUidReturnsFalseForUnknown() {
        final boolean exists = collectorInstanceService.existsByInstanceUid("non-existent-uid");

        assertThat(exists).isFalse();
    }

    @Test
    void countByFleetGroupedReturnsPerFleetCounts() throws Exception {
        final Instant now = Instant.now();
        final Instant recentlySeen = now.minusSeconds(30);
        final Instant longAgo = now.minusSeconds(600);
        final Instant onlineThreshold = now.minusSeconds(60);

        final String fleetA = "507f1f77bcf86cd799439012";
        final String fleetB = "507f1f77bcf86cd799439013";

        // fleet-a: 3 instances (2 online, 1 offline based on threshold)
        enrollWithFleetAndLastSeen("uid-a1", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a2", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a3", fleetA, longAgo);

        // fleet-b: 1 instance (1 online)
        enrollWithFleetAndLastSeen("uid-b1", fleetB, recentlySeen);

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
    void findByInstanceUidsReturnsMappedResults() throws Exception {
        enroll("uid-1");
        enroll("uid-2");
        enroll("uid-3");

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
    void deleteByInstanceUidDeletesExistingInstance() throws Exception {
        enroll("uid-to-delete");

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
    void deleteExpiredRemovesOldInstances() throws Exception {
        final Instant now = Instant.now();
        final Duration threshold = Duration.ofDays(7);

        // Expired: last seen 8 days ago
        enrollWithFleetAndLastSeen("uid-expired", "507f1f77bcf86cd799439012", now.minus(Duration.ofDays(8)));
        // Not expired: last seen 3 days ago
        enrollWithFleetAndLastSeen("uid-recent", "507f1f77bcf86cd799439012", now.minus(Duration.ofDays(3)));

        final long deleted = collectorInstanceService.deleteExpired(threshold);

        assertThat(deleted).isEqualTo(1);
        assertThat(collectorInstanceService.findByInstanceUid("uid-expired")).isEmpty();
        assertThat(collectorInstanceService.findByInstanceUid("uid-recent")).isPresent();
    }

    @Test
    void deleteExpiredReturnsZeroWhenNothingToDelete() throws Exception {
        enrollWithFleetAndLastSeen("uid-fresh",
                "507f1f77bcf86cd799439012", Instant.now());

        final long deleted = collectorInstanceService.deleteExpired(Duration.ofDays(7));

        assertThat(deleted).isEqualTo(0);
    }

    @Test
    void findByActiveOrNextFingerprintMatchesActiveFingerprint() throws Exception {
        final var instance = enroll("uid-active");

        final var found = collectorInstanceService.findByActiveOrNextFingerprint(instance.activeCertificateFingerprint());

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("uid-active");
    }

    @Test
    void findByActiveOrNextFingerprintMatchesNextFingerprint() throws Exception {
        enroll("uid-next");
        setNextCertificateFields("uid-next", "sha256:next-fp", "next-cert-pem",
                Instant.now().plus(Duration.ofDays(30)));

        final var found = collectorInstanceService.findByActiveOrNextFingerprint("sha256:next-fp");

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("uid-next");
        assertThat(found.get().nextCertificateFingerprint()).hasValue("sha256:next-fp");
    }

    @Test
    void findByActiveOrNextFingerprintReturnsEmptyForUnknown() throws Exception {
        enroll("uid-no-match");

        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByActiveOrNextFingerprint("sha256:unknown-fp");

        assertThat(found).isEmpty();
    }

    @Test
    void activateNextCertificatePromotesNextToActive() throws Exception {
        enroll("uid-activate");
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
    void activateNextCertificateThrowsWhenNextFieldsMissing() throws Exception {
        final CollectorInstanceDTO enrolled = enroll("uid-no-next");

        assertThatThrownBy(() -> collectorInstanceService.activateNextCertificate(enrolled))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void insertNextCertificateSetsNextFields() throws Exception {
        final var instance = enroll("uid-insert-next");
        final var nextDuration = Duration.ofDays(30);
        final var nextExpiresAt = Instant.now().plus(nextDuration);

        final var nextCert = certBuilder.createEndEntityCert("uid-insert-next", issuerCert,
                KeyUsage.digitalSignature, nextDuration);

        final var result = collectorInstanceService.insertNextCertificate(
                "uid-insert-next", nextCert.fingerprint(), nextCert.certificate(), nextExpiresAt);

        // Verify BSON type is Date, not String
        assertFieldIsDate("uid-insert-next", CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_EXPIRES_AT);

        assertThat(result).isTrue();
        final var updated = collectorInstanceService.findByInstanceUid("uid-insert-next").orElseThrow();
        assertThat(updated.nextCertificateFingerprint()).hasValue(nextCert.fingerprint());
        assertThat(updated.nextCertificatePem()).hasValue(nextCert.certificate());
        assertThat(updated.nextCertificateExpiresAt()).hasValue(Date.from(nextExpiresAt).toInstant());
        // Active certificate should remain unchanged
        assertThat(updated.activeCertificateFingerprint()).isEqualTo(instance.activeCertificateFingerprint());
    }

    @Test
    void insertNextCertificateReturnsFalseForNonExistentInstance() {
        final boolean result = collectorInstanceService.insertNextCertificate(
                "non-existent-uid", "sha256:next-fp", "next-cert-pem", Instant.now().plus(Duration.ofDays(30)));

        assertThat(result).isFalse();
    }

    @Test
    void insertNextCertificateOverwritesPreviousNextFields() throws Exception {
        enroll("uid-overwrite-next");
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

    @Test
    void updateFromReportThrowsForNonExistentInstance() {
        final var report = CollectorInstanceReport.builder()
                .instanceUid("new-uid")
                .messageSeqNum(1L)
                .capabilities(100L)
                .build();

        assertThatThrownBy(() -> collectorInstanceService.updateFromReport(report))
                .hasMessageContaining("enrolled")
                .isInstanceOf(IllegalArgumentException.class);

        // Does NOT create a new instance document
        assertThat(collectorInstanceService.findByInstanceUid("new-uid")).isEmpty();
    }

    @Test
    void updateFromReportReturnsPreviousState() throws Exception {
        final var uid = "returning-uid";
        final var fleetId = "000000000000000000000000";

        // Enroll first so the document exists with a fleet_id
        enroll(uid);

        // First report — document already exists from enroll, so we get its state back
        final var firstReport = CollectorInstanceReport.builder()
                .instanceUid(uid)
                .messageSeqNum(1L)
                .capabilities(100L)
                .lastProcessedTxnSeq(0L)
                .build();
        collectorInstanceService.updateFromReport(firstReport);

        // Second report — should return the state written by the first report
        final var secondReport = CollectorInstanceReport.builder()
                .instanceUid(uid)
                .messageSeqNum(2L)
                .capabilities(200L)
                .lastProcessedTxnSeq(1L)
                .build();

        final var previousState = collectorInstanceService.updateFromReport(secondReport);

        assertThat(previousState).isNotNull();
        assertThat(previousState.messageSeqNum()).isEqualTo(1L);
        assertThat(previousState.lastProcessTxnSeq()).isEqualTo(0L);
        assertThat(previousState.fleetId()).isEqualTo(fleetId);
        assertThat(previousState.osType()).isEqualTo(CollectorOSType.UNKNOWN);
    }

    @Test
    void updateFromReportUpdatesExistingDocument() throws Exception {
        final var uid = "update-uid";
        enroll(uid);

        final var firstReport = CollectorInstanceReport.builder()
                .instanceUid(uid)
                .messageSeqNum(1L)
                .capabilities(100L)
                .nonIdentifyingAttributes(List.of(Attribute.of("os.type", "linux")))
                .lastSeen(Instant.ofEpochSecond(0))
                .build();
        final var prevState1 = collectorInstanceService.updateFromReport(firstReport);

        assertThat(prevState1.osType()).isEqualTo(CollectorOSType.UNKNOWN);

        assertThat(collectorInstanceService.findByInstanceUid(uid)).hasValueSatisfying(instance -> {
            assertThat(instance.messageSeqNum()).isEqualTo(1L);
            assertThat(instance.capabilities()).isEqualTo(100L);
            assertThat(instance.nonIdentifyingAttributes()).hasValueSatisfying(attrs -> {
                assertThat(attrs).extracting(Attribute::key).containsExactly("os.type");
                assertThat(attrs).extracting(a -> String.valueOf(a.value())).containsExactly("linux");
            });
            assertThat(instance.lastSeen()).isEqualTo(Instant.ofEpochSecond(0));
        });

        final var secondReport = CollectorInstanceReport.builder()
                .instanceUid(uid)
                .messageSeqNum(2L)
                .capabilities(300L)
                .nonIdentifyingAttributes(List.of(
                        Attribute.of("os.type", "windows"),
                        Attribute.of("host.arch", "amd64")
                ))
                .lastSeen(Instant.ofEpochSecond(100))
                .build();
        final var prevState2 = collectorInstanceService.updateFromReport(secondReport);

        assertThat(prevState2.osType()).isEqualTo(CollectorOSType.LINUX);

        assertThat(collectorInstanceService.findByInstanceUid(uid)).hasValueSatisfying(instance -> {
            assertThat(instance.messageSeqNum()).isEqualTo(2L);
            assertThat(instance.capabilities()).isEqualTo(300L);
            assertThat(instance.nonIdentifyingAttributes()).hasValueSatisfying(attrs -> {
                assertThat(attrs).extracting(Attribute::key).containsExactly("os.type", "host.arch");
                assertThat(attrs).extracting(a -> (String) a.value()).containsExactly("windows", "amd64");
            });
            assertThat(instance.lastSeen()).isEqualTo(Instant.ofEpochSecond(100));
        });
    }

    @Test
    void updateFromReportDoesNotOverwriteOptionalFieldsWhenAbsent() throws Exception {
        final var uid = "optional-uid";
        enroll(uid);

        // First report sets attributes
        final var firstReport = CollectorInstanceReport.builder()
                .instanceUid(uid)
                .lastSeen(Instant.ofEpochSecond(0))
                .messageSeqNum(1L)
                .capabilities(100L)
                .lastProcessedTxnSeq(5L)
                .identifyingAttributes(List.of(Attribute.of("service.name", "supervisor")))
                .nonIdentifyingAttributes(List.of(Attribute.of("os.type", "linux")))
                .build();
        collectorInstanceService.updateFromReport(firstReport);

        // Second report omits optional fields
        final var secondReport = CollectorInstanceReport.builder()
                .instanceUid(uid)
                .lastSeen(Instant.ofEpochSecond(1))
                .messageSeqNum(2L)
                .capabilities(200L)
                .build();
        collectorInstanceService.updateFromReport(secondReport);

        // The optional fields from the first report should still be present
        assertThat(collectorInstanceService.findByInstanceUid(uid)).hasValueSatisfying(instance -> {
            assertThat(instance.messageSeqNum()).isEqualTo(2L);
            assertThat(instance.capabilities()).isEqualTo(200L);
            assertThat(instance.identifyingAttributes())
                    .hasValueSatisfying(attrs -> assertThat(attrs).containsExactly(Attribute.of("service.name", "supervisor")));
            assertThat(instance.nonIdentifyingAttributes())
                    .hasValueSatisfying(attrs -> assertThat(attrs).contains(Attribute.of("os.type", "linux")));
            assertThat(instance.lastSeen()).isEqualTo(Instant.ofEpochSecond(1));
            // lastProcessedTxnSeq should remain from the first report since the second didn't set it
            assertThat(instance.lastProcessedTxnSeq()).isEqualTo(5L);
        });
    }

    @Test
    void updateFromReportStoresLastSeenAsDate() throws Exception {
        final var uid = "date-uid";
        enroll(uid);

        final var report = CollectorInstanceReport.builder()
                .instanceUid("date-uid")
                .messageSeqNum(1L)
                .capabilities(100L)
                .build();

        collectorInstanceService.updateFromReport(report);

        assertFieldIsDate("date-uid", CollectorInstanceDTO.FIELD_LAST_SEEN);
    }

    @Test
    void extractOsTypeFromReportReturnsCorrectOs() {
        final var report = reportWithAttributes(List.of(
                Attribute.of("host.name", "h1"),
                Attribute.of("os.type", "linux")
        ));

        assertThat(CollectorInstanceService.extractOsTypeFromReport(report)).isEqualTo(CollectorOSType.LINUX);
    }

    @Test
    void extractOsTypeFromReportReturnsUnknownForUnrecognizedOsType() {
        final var report = reportWithAttributes(List.of(Attribute.of("os.type", "freebsd")));

        assertThat(CollectorInstanceService.extractOsTypeFromReport(report)).isEqualTo(CollectorOSType.UNKNOWN);
    }

    @Test
    void extractOsTypeFromReportReturnsUnknownWhenOsTypeAttributeMissing() {
        final var report = reportWithAttributes(List.of(
                Attribute.of("host.name", "h1"),
                Attribute.of("host.arch", "amd64")
        ));

        assertThat(CollectorInstanceService.extractOsTypeFromReport(report)).isEqualTo(CollectorOSType.UNKNOWN);
    }

    @Test
    void extractOsTypeFromReportReturnsUnknownWhenAttributesAbsent() {
        final var report = CollectorInstanceReport.builder()
                .instanceUid("uid-1")
                .messageSeqNum(1L)
                .capabilities(0L)
                .build();

        assertThat(CollectorInstanceService.extractOsTypeFromReport(report)).isEqualTo(CollectorOSType.UNKNOWN);
    }

    @Test
    void extractOsTypeFromReportReturnsUnknownWhenAttributesEmpty() {
        final var report = reportWithAttributes(List.of());

        assertThat(CollectorInstanceService.extractOsTypeFromReport(report)).isEqualTo(CollectorOSType.UNKNOWN);
    }

    private static CollectorInstanceReport reportWithAttributes(List<Attribute> attributes) {
        return CollectorInstanceReport.builder()
                .instanceUid("uid-1")
                .messageSeqNum(1L)
                .capabilities(0L)
                .nonIdentifyingAttributes(attributes)
                .build();
    }

    private CollectorInstanceDTO enroll(String instanceUid) throws Exception {
        final var cert = certBuilder.createEndEntityCert(instanceUid, issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));

        return collectorInstanceService.enroll(
                instanceUid,
                "000000000000000000000000",
                cert.fingerprint(),
                cert.certificate(),
                Date.from(cert.notAfter()),
                issuerCert.id(),
                Instant.now(),
                "000000000000000000000000"
        );
    }

    private void enrollWithFleetAndLastSeen(String instanceUid,
                                            String fleetId,
                                            Instant lastSeen) throws Exception {
        final var cert = certBuilder.createEndEntityCert(instanceUid, issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));

        collectorInstanceService.enroll(
                instanceUid,
                fleetId,
                cert.fingerprint(),
                cert.certificate(),
                Date.from(cert.notAfter()),
                issuerCert.id(),
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
