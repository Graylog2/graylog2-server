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

import com.google.common.eventbus.Subscribe;
import com.mongodb.client.model.Filters;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bson.Document;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorOSType;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.collectors.events.CollectorInstanceCertsChangedEvent;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.testing.TestClocks;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.threeten.extra.MutableClock;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    private MutableClock clock;
    private List<CollectorInstanceCertsChangedEvent> capturedEvents;

    @BeforeAll
    static void beforeAll() throws Exception {
        certBuilder = new CertificateBuilder(encryptedValueService, "Graylog", TestClocks.fixedEpoch());
        final CertificateEntry caCert = certBuilder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(7)).withId("100000000000000000000000");
        issuerCert = certBuilder.createIntermediateCa("Test Issuer", caCert, Duration.ofDays(5)).withId("200000000000000000000000");
    }

    @BeforeEach
    void setUp(MongoCollections coll) {
        mongoCollections = coll;
        clock = TestClocks.mutableFixedEpoch();
        capturedEvents = new ArrayList<>();

        // directExecutor() makes posts synchronous, so events are captured inline.
        final var clusterEventBus = new ClusterEventBus();
        clusterEventBus.registerClusterEventSubscriber(new Object() {
            @Subscribe
            public void on(CollectorInstanceCertsChangedEvent event) {
                capturedEvents.add(event);
            }
        });

        collectorInstanceService = new CollectorInstanceService(coll, clusterEventBus, clock);
    }

    private CollectorInstanceCertsChangedEvent lastEvent() {
        assertThat(capturedEvents).isNotEmpty();
        return capturedEvents.get(capturedEvents.size() - 1);
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
    void countByFleetGroupedReturnsPerFleetCounts() throws Exception {
        final Instant reference = Instant.parse("2025-01-01T00:00:00Z");
        final Instant recentlySeen = reference.minusSeconds(30);
        final Instant longAgo = reference.minusSeconds(600);
        final Instant onlineThreshold = reference.minusSeconds(60);

        final String fleetA = "507f1f77bcf86cd799439012";
        final String fleetB = "507f1f77bcf86cd799439013";

        // fleet-a: 3 instances (2 online, 1 offline based on threshold)
        enrollWithFleetAndLastSeen("uid-a1", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a2", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a3", fleetA, longAgo);

        // fleet-b: 1 instance (1 online)
        enrollWithFleetAndLastSeen("uid-b1", fleetB, recentlySeen);

        final var grouped = collectorInstanceService.countByFleetGrouped(onlineThreshold);

        assertThat(grouped).containsKey(fleetA);
        assertThat(grouped.get(fleetA).total()).isEqualTo(3L);
        assertThat(grouped.get(fleetA).online()).isEqualTo(2L);
        assertThat(grouped.get(fleetA).offline()).isEqualTo(1L);

        assertThat(grouped).containsKey(fleetB);
        assertThat(grouped.get(fleetB).total()).isEqualTo(1L);
        assertThat(grouped.get(fleetB).online()).isEqualTo(1L);
        assertThat(grouped.get(fleetB).offline()).isZero();

        // absent fleet should not be in the map
        assertThat(grouped).doesNotContainKey("507f1f77bcf86cd799439099");
    }

    @Test
    void countAcrossAllFleetsReturnsZeroForEmptyCollection() {
        final var stats = collectorInstanceService.countAcrossAllFleets(Instant.now().minusSeconds(60));

        assertThat(stats.total()).isZero();
        assertThat(stats.online()).isZero();
    }

    @Test
    void countAcrossAllFleetsSumsAcrossFleetsAndStatuses() throws Exception {
        final Instant now = Instant.now();
        final Instant recentlySeen = now.minusSeconds(30);
        final Instant longAgo = now.minusSeconds(600);
        final Instant onlineThreshold = now.minusSeconds(60);

        final String fleetA = "507f1f77bcf86cd799439012";
        final String fleetB = "507f1f77bcf86cd799439013";

        // fleet-a: 3 instances, 2 online + 1 offline
        enrollWithFleetAndLastSeen("uid-a1", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a2", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a3", fleetA, longAgo);

        // fleet-b: 2 instances, 1 online + 1 offline
        enrollWithFleetAndLastSeen("uid-b1", fleetB, recentlySeen);
        enrollWithFleetAndLastSeen("uid-b2", fleetB, longAgo);

        final var stats = collectorInstanceService.countAcrossAllFleets(onlineThreshold);

        assertThat(stats.total()).isEqualTo(5L);
        assertThat(stats.online()).isEqualTo(3L);
        assertThat(stats.offline()).isEqualTo(2L);
    }

    @Test
    void countAcrossAllFleetsTreatsInstanceAtThresholdAsOnline() throws Exception {
        final Instant onlineThreshold = Instant.now().minusSeconds(60);

        enrollWithFleetAndLastSeen("uid-at-threshold", "507f1f77bcf86cd799439012", onlineThreshold);
        enrollWithFleetAndLastSeen("uid-just-below", "507f1f77bcf86cd799439012", onlineThreshold.minusMillis(1));

        final var stats = collectorInstanceService.countAcrossAllFleets(onlineThreshold);

        assertThat(stats.total()).isEqualTo(2L);
        assertThat(stats.online()).isEqualTo(1L);
    }

    @Test
    void countByFleetReturnsZeroForUnknownFleet() throws Exception {
        // Populate the collection so we can be sure the zero result is fleet-scoped,
        // not just an empty-collection fallback.
        enrollWithFleetAndLastSeen("uid-other", "507f1f77bcf86cd799439012", Instant.now());

        final var stats = collectorInstanceService.countByFleet(
                "507f1f77bcf86cd799439099", Instant.now().minusSeconds(60));

        assertThat(stats.total()).isZero();
        assertThat(stats.online()).isZero();
        assertThat(stats.offline()).isZero();
    }

    @Test
    void countByFleetIsolatesToRequestedFleet() throws Exception {
        final Instant now = Instant.now();
        final Instant recentlySeen = now.minusSeconds(30);
        final Instant longAgo = now.minusSeconds(600);
        final Instant onlineThreshold = now.minusSeconds(60);

        final String fleetA = "507f1f77bcf86cd799439012";
        final String fleetB = "507f1f77bcf86cd799439013";

        // fleet-a: 2 online + 1 offline
        enrollWithFleetAndLastSeen("uid-a1", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a2", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen("uid-a3", fleetA, longAgo);
        // fleet-b: noise that must NOT be counted in fleet-a's totals
        enrollWithFleetAndLastSeen("uid-b1", fleetB, recentlySeen);
        enrollWithFleetAndLastSeen("uid-b2", fleetB, longAgo);

        final var stats = collectorInstanceService.countByFleet(fleetA, onlineThreshold);

        assertThat(stats.total()).isEqualTo(3L);
        assertThat(stats.online()).isEqualTo(2L);
        assertThat(stats.offline()).isEqualTo(1L);
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
        final Instant reference = Instant.parse("2025-01-01T00:00:00Z");
        final Duration threshold = Duration.ofDays(7);

        // Expired: last seen 8 days before reference
        enrollWithFleetAndLastSeen("uid-expired", "507f1f77bcf86cd799439012", reference.minus(Duration.ofDays(8)));
        // Not expired: last seen 3 days before reference
        enrollWithFleetAndLastSeen("uid-recent", "507f1f77bcf86cd799439012", reference.minus(Duration.ofDays(3)));

        clock.setInstant(reference);
        final long deleted = collectorInstanceService.deleteExpired(threshold);

        assertThat(deleted).isEqualTo(1);
        assertThat(collectorInstanceService.findByInstanceUid("uid-expired")).isEmpty();
        assertThat(collectorInstanceService.findByInstanceUid("uid-recent")).isPresent();
    }

    @Test
    void deleteExpiredReturnsZeroWhenNothingToDelete() throws Exception {
        final Instant reference = Instant.parse("2025-01-01T00:00:00Z");
        enrollWithFleetAndLastSeen("uid-fresh", "507f1f77bcf86cd799439012", reference);

        clock.setInstant(reference);
        final long deleted = collectorInstanceService.deleteExpired(Duration.ofDays(7));

        assertThat(deleted).isEqualTo(0);
    }

    @Test
    void reEnrollUpdatesCertTokenAndLastSeen() throws Exception {
        final Instant enrollTime = Instant.parse("2025-01-01T00:00:00Z");
        clock.setInstant(enrollTime);
        final var original = enroll("uid-re");

        final Instant reEnrollTime = enrollTime.plus(Duration.ofDays(30));
        clock.setInstant(reEnrollTime);

        final var newCert = certBuilder.createEndEntityCert("uid-re", issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var newIssued = new IssuedCertificate(newCert.fingerprint(), newCert.certificate(), newCert.notAfter(), issuerCert.id());

        final var updated = collectorInstanceService.reEnroll(original, newIssued, "new-token-id");

        assertThat(updated.activeCertificateFingerprint()).isEqualTo(newIssued.fingerprint());
        assertThat(updated.activeCertificatePem()).isEqualTo(newIssued.certPem());
        assertThat(updated.activeCertificateExpiresAt()).isEqualTo(newIssued.notAfter());
        assertThat(updated.issuingCaId()).isEqualTo(issuerCert.id());
        assertThat(updated.enrollmentTokenId()).isEqualTo("new-token-id");
        assertThat(updated.lastSeen()).isEqualTo(reEnrollTime);
    }

    @Test
    void reEnrollPreservesExistingFields() throws Exception {
        final Instant enrollTime = Instant.parse("2025-01-01T00:00:00Z");
        final var original = enrollWithFleetAndLastSeen("uid-preserve", "507f1f77bcf86cd799439012", enrollTime);

        clock.setInstant(enrollTime.plus(Duration.ofDays(30)));
        final var newCert = certBuilder.createEndEntityCert("uid-preserve", issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var newIssued = new IssuedCertificate(newCert.fingerprint(), newCert.certificate(), newCert.notAfter(), issuerCert.id());

        final var updated = collectorInstanceService.reEnroll(original, newIssued, "new-token-id");

        assertThat(updated.instanceUid()).isEqualTo(original.instanceUid());
        assertThat(updated.enrolledAt()).isEqualTo(enrollTime);
        assertThat(updated.messageSeqNum()).isEqualTo(original.messageSeqNum());
        assertThat(updated.capabilities()).isEqualTo(original.capabilities());
        assertThat(updated.fleetId()).isEqualTo("507f1f77bcf86cd799439012");
    }

    @Test
    void reEnrollClearsNextCertificateFields() throws Exception {
        final var original = enroll("uid-pending-renewal");
        setNextCertificateFields("uid-pending-renewal", "sha256:next-fp", "next-cert-pem",
                Instant.parse("2030-01-01T00:00:00Z"));

        final var newCert = certBuilder.createEndEntityCert("uid-pending-renewal", issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var newIssued = new IssuedCertificate(newCert.fingerprint(), newCert.certificate(), newCert.notAfter(), issuerCert.id());

        final var withNext = collectorInstanceService.findByInstanceUid("uid-pending-renewal").orElseThrow();
        final var updated = collectorInstanceService.reEnroll(withNext, newIssued, "token-x");

        assertThat(updated.nextCertificateFingerprint()).isEmpty();
        assertThat(updated.nextCertificatePem()).isEmpty();
        assertThat(updated.nextCertificateExpiresAt()).isEmpty();
    }

    @Test
    void enrollAndReEnrollRoundTripThroughMongo() throws Exception {
        final Instant enrollTime = Instant.parse("2025-01-01T00:00:00Z");
        final Instant reEnrollTime = Instant.parse("2025-02-14T10:30:45Z");

        clock.setInstant(enrollTime);
        final var enrolled = enroll("uid-roundtrip");

        // Fresh read after enroll — exercises the deserialize path independently of the insert's in-memory DTO.
        final var afterEnroll = collectorInstanceService.findByInstanceUid("uid-roundtrip").orElseThrow();
        assertThat(afterEnroll.enrolledAt()).isEqualTo(enrollTime);
        assertThat(afterEnroll.lastSeen()).isEqualTo(enrollTime);
        assertThat(afterEnroll.activeCertificateExpiresAt()).isEqualTo(enrolled.activeCertificateExpiresAt());

        clock.setInstant(reEnrollTime);
        final var newCert = certBuilder.createEndEntityCert("uid-roundtrip", issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var newIssued = new IssuedCertificate(newCert.fingerprint(), newCert.certificate(), newCert.notAfter(), issuerCert.id());
        collectorInstanceService.reEnroll(enrolled, newIssued, "token-2");

        // Fresh read after re-enroll — exercises the post-update deserialize path.
        final var afterReEnroll = collectorInstanceService.findByInstanceUid("uid-roundtrip").orElseThrow();
        assertThat(afterReEnroll.enrolledAt()).isEqualTo(enrollTime);
        assertThat(afterReEnroll.lastSeen()).isEqualTo(reEnrollTime);
        assertThat(afterReEnroll.activeCertificateExpiresAt()).isEqualTo(newIssued.notAfter());
        assertThat(afterReEnroll.activeCertificateFingerprint()).isEqualTo(newIssued.fingerprint());
        assertThat(afterReEnroll.activeCertificatePem()).isEqualTo(newIssued.certPem());
        assertThat(afterReEnroll.fleetId()).isEqualTo("000000000000000000000000");
        assertThat(afterReEnroll.enrollmentTokenId()).isEqualTo("token-2");
    }

    @Test
    void reEnrollStoresTemporalFieldsAsBsonDate() throws Exception {
        final var uid = "uid-bson-types";
        final var original = enroll(uid);

        final var newCert = certBuilder.createEndEntityCert(uid, issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var newIssued = new IssuedCertificate(newCert.fingerprint(), newCert.certificate(), newCert.notAfter(), issuerCert.id());

        collectorInstanceService.reEnroll(original, newIssued, "token-x");

        assertFieldIsDate(uid, CollectorInstanceDTO.FIELD_LAST_SEEN);
        assertFieldIsDate(uid, CollectorInstanceDTO.FIELD_ACTIVE_CERTIFICATE_EXPIRES_AT);
    }

    @Test
    void enrollDoesNotPersistNextCertificateFingerprintAsNull() throws Exception {
        // The partial index on next_certificate_fingerprint filters on {$exists: true}, which in MongoDB
        // also matches a field present with an explicit null. Persisting the field as null (instead of
        // omitting it) would pull every non-renewing instance into the index, defeating its purpose. A
        // freshly enrolled instance has no next certificate, so the field must be absent from the document.
        enroll("uid-no-next-field");

        final var doc = findRawDocument("uid-no-next-field").orElseThrow();
        assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_FINGERPRINT)).isFalse();
    }

    @Test
    void enrollDoesNotPersistPreviousCertificateFieldsAsNull() throws Exception {
        // A freshly enrolled instance has never rotated, so none of the previous-certificate fields nor
        // certificates_rotated_at exist yet. They must be ABSENT, not present-with-null: the partial index
        // on previous_certificate_fingerprint is UNIQUE, and {$exists: true} matches an explicit null, so a
        // second instance persisting a null previous fingerprint would collide and fail the write.
        enroll("uid-no-previous-fields");

        final var doc = findRawDocument("uid-no-previous-fields").orElseThrow();
        assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT)).isFalse();
        assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_PEM)).isFalse();
        assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_EXPIRES_AT)).isFalse();
        assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_CERTIFICATES_ROTATED_AT)).isFalse();
    }

    @Test
    void enrollingMultipleInstancesWithoutPreviousDoesNotCollideOnUniqueIndex() throws Exception {
        // Guards the unique partial index on previous_certificate_fingerprint: instances that have never
        // rotated omit the field entirely, so the partial {$exists: true} filter excludes them and many
        // such instances can coexist. If any write path ever wrote an explicit null instead, the second
        // enroll here would fail with a duplicate-key error.
        final var first = enroll("uid-no-previous-1");
        final var second = enroll("uid-no-previous-2");

        assertThat(first.id()).isNotNull();
        assertThat(second.id()).isNotNull();
        assertThat(collectorInstanceService.findByInstanceUid("uid-no-previous-1")).isPresent();
        assertThat(collectorInstanceService.findByInstanceUid("uid-no-previous-2")).isPresent();
    }

    @Test
    void constructionClearsLegacyNullNextCertificateValuesSoTheUniqueIndexCanBuild() throws Exception {
        // Released versions persisted next_certificate_fingerprint as an explicit null (the field predates
        // @JsonInclude(NON_ABSENT)). Reproduce that pre-migration state: drop the unique next index so
        // multiple null documents can be seeded (the live unique index would reject the second null), then
        // seed several. On construction the service must clear the nulls and successfully (re)build the index.
        final var raw = mongoCollections.nonEntityCollection(CollectorInstanceService.COLLECTION_NAME, Document.class);
        raw.dropIndex("next_certificate_fingerprint_1");
        for (int i = 0; i < 3; i++) {
            enroll("uid-legacy-null-" + i);
            raw.updateOne(Filters.eq(CollectorInstanceDTO.FIELD_INSTANCE_UID, "uid-legacy-null-" + i),
                    new Document("$set", new Document()
                            .append(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_FINGERPRINT, null)
                            .append(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_PEM, null)
                            .append(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_EXPIRES_AT, null)));
        }

        // Constructing the service runs the cleanup, then rebuilds the indexes over the now-clean data.
        new CollectorInstanceService(mongoCollections, new ClusterEventBus(), clock);

        for (int i = 0; i < 3; i++) {
            final var doc = findRawDocument("uid-legacy-null-" + i).orElseThrow();
            assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_FINGERPRINT)).isFalse();
            assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_PEM)).isFalse();
            assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_EXPIRES_AT)).isFalse();
        }

        // The unique next index must exist again — if the cleanup had failed, createIndexes would have
        // collided on the nulls (swallowed by its catch) and the index would be missing.
        boolean uniqueNextIndexPresent = false;
        for (Document index : raw.listIndexes()) {
            final Document key = index.get("key", Document.class);
            if (key != null && key.containsKey(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_FINGERPRINT)
                    && index.getBoolean("unique", false)) {
                uniqueNextIndexPresent = true;
                break;
            }
        }
        assertThat(uniqueNextIndexPresent).isTrue();
    }

    @Test
    void activateNextCertificateRemovesNextCertificateFingerprintField() throws Exception {
        // Activation promotes next -> active and must remove the next fingerprint field entirely (not set
        // it to null), so the instance drops back out of the partial index once it is no longer mid-renewal.
        enroll("uid-activate-clears-next");
        collectorInstanceService.insertNextCertificate("uid-activate-clears-next", "sha256:next-fp", "next-pem",
                Instant.ofEpochMilli(0).plus(Duration.ofDays(2)));
        final var withNext = collectorInstanceService.findByInstanceUid("uid-activate-clears-next").orElseThrow();

        collectorInstanceService.activateNextCertificate(withNext);

        final var doc = findRawDocument("uid-activate-clears-next").orElseThrow();
        assertThat(doc.containsKey(CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_FINGERPRINT)).isFalse();
    }

    @Test
    void reEnrollThrowsWhenRecordDoesNotExist() throws Exception {
        final var cert = certBuilder.createEndEntityCert("ghost", issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var issued = new IssuedCertificate(cert.fingerprint(), cert.certificate(), cert.notAfter(), issuerCert.id());

        // An instance whose id matches no stored record.
        final var ghost = enroll("ghost").toBuilder().id("507f1f77bcf86cd799439999").build();

        assertThatThrownBy(() -> collectorInstanceService.reEnroll(ghost, issued, "token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("doesn't exist");
    }

    @Test
    void reEnrollThrowsWhenActiveCertificateChangedConcurrently() throws Exception {
        final var original = enroll("uid-cas");

        final var newCert = certBuilder.createEndEntityCert("uid-cas", issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var newIssued = new IssuedCertificate(newCert.fingerprint(), newCert.certificate(), newCert.notAfter(), issuerCert.id());

        // Stale fingerprint: simulates the active cert being swapped (e.g. by a concurrent renewal
        // activation) between the caller's read and the update.
        final var stale = original.toBuilder().activeCertificateFingerprint("sha256:stale-fingerprint").build();
        assertThatThrownBy(() -> collectorInstanceService.reEnroll(stale, newIssued, "token"))
                .isInstanceOf(IllegalStateException.class);

        // The record must be untouched — same cert, token, and next_* state as before.
        final var unchanged = collectorInstanceService.findByInstanceUid("uid-cas").orElseThrow();
        assertThat(unchanged.activeCertificateFingerprint()).isEqualTo(original.activeCertificateFingerprint());
        assertThat(unchanged.activeCertificatePem()).isEqualTo(original.activeCertificatePem());
        assertThat(unchanged.enrollmentTokenId()).isEqualTo(original.enrollmentTokenId());
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

    // ----- CollectorInstanceCertsChangedEvent publishing -----

    @Test
    void enrollPublishesActiveFingerprint() throws Exception {
        final var instance = enroll("uid-evt-enroll");

        assertThat(lastEvent().fingerprints()).containsExactly(instance.activeCertificateFingerprint());
    }

    @Test
    void insertNextCertificatePublishesActiveAndNextFingerprints() throws Exception {
        final var enrolled = enroll("uid-evt-next");
        capturedEvents.clear();

        collectorInstanceService.insertNextCertificate("uid-evt-next", "sha256:next-fp", "pem",
                Instant.now().plus(Duration.ofDays(30)));

        assertThat(lastEvent().fingerprints())
                .containsExactlyInAnyOrder(enrolled.activeCertificateFingerprint(), "sha256:next-fp");
    }

    @Test
    void insertNextCertificatePublishesReplacedAndNewNextFingerprints() throws Exception {
        final var enrolled = enroll("uid-evt-next-replace");
        collectorInstanceService.insertNextCertificate("uid-evt-next-replace", "sha256:first-next", "pem1",
                Instant.now().plus(Duration.ofDays(10)));
        capturedEvents.clear();

        collectorInstanceService.insertNextCertificate("uid-evt-next-replace", "sha256:second-next", "pem2",
                Instant.now().plus(Duration.ofDays(20)));

        assertThat(lastEvent().fingerprints()).containsExactlyInAnyOrder(
                enrolled.activeCertificateFingerprint(), "sha256:first-next", "sha256:second-next");
    }

    @Test
    void activateNextCertificatePublishesRotatedFingerprints() throws Exception {
        final var enrolled = enroll("uid-evt-activate");
        setNextCertificateFields("uid-evt-activate", "sha256:new-active", "new-pem",
                Instant.now().plus(Duration.ofDays(30)));
        final var withNext = collectorInstanceService.findByInstanceUid("uid-evt-activate").orElseThrow();
        capturedEvents.clear();

        collectorInstanceService.activateNextCertificate(withNext);

        // Both rotated fingerprints (demoted old active + promoted next) are touched and re-resolved.
        assertThat(lastEvent().fingerprints())
                .containsExactlyInAnyOrder(enrolled.activeCertificateFingerprint(), "sha256:new-active");
    }

    @Test
    void activateNextCertificateDemotesActiveToPreviousAndStampsRotationTimestamp() throws Exception {
        final var enrolled = enroll("uid-rotate-fields");
        setNextCertificateFields("uid-rotate-fields", "sha256:new-active", "new-pem",
                Instant.now().plus(Duration.ofDays(30)));
        final var withNext = collectorInstanceService.findByInstanceUid("uid-rotate-fields").orElseThrow();

        collectorInstanceService.activateNextCertificate(withNext);

        // The old active cert is demoted into the previous slot, and the rotation is timestamped so the
        // ingest grace window can be measured from it. Temporal fields must be stored as BSON dates.
        final var doc = findRawDocument("uid-rotate-fields").orElseThrow();
        assertThat(doc.getString(CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT))
                .isEqualTo(enrolled.activeCertificateFingerprint());
        assertFieldIsDate("uid-rotate-fields", CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_EXPIRES_AT);
        assertFieldIsDate("uid-rotate-fields", CollectorInstanceDTO.FIELD_CERTIFICATES_ROTATED_AT);
    }

    @Test
    void reEnrollPublishesOldAndNewActiveFingerprints() throws Exception {
        final var original = enroll("uid-evt-reenroll");
        final var newCert = certBuilder.createEndEntityCert("uid-evt-reenroll", issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var newIssued = new IssuedCertificate(newCert.fingerprint(), newCert.certificate(), newCert.notAfter(), issuerCert.id());
        capturedEvents.clear();

        collectorInstanceService.reEnroll(original, newIssued, "token-evt");

        assertThat(lastEvent().fingerprints())
                .containsExactlyInAnyOrder(original.activeCertificateFingerprint(), newIssued.fingerprint());
    }

    @Test
    void deleteByInstanceUidPublishesAllCertFingerprints() throws Exception {
        final var instance = enroll("uid-evt-delete");
        setNextCertificateFields("uid-evt-delete", "sha256:pending", "pending-pem",
                Instant.now().plus(Duration.ofDays(30)));
        capturedEvents.clear();

        collectorInstanceService.deleteByInstanceUid("uid-evt-delete");

        assertThat(lastEvent().fingerprints())
                .containsExactlyInAnyOrder(instance.activeCertificateFingerprint(), "sha256:pending");
    }

    @Test
    void deleteExpiredPublishesFingerprintsForPurgedInstances() throws Exception {
        final Instant reference = Instant.parse("2025-01-01T00:00:00Z");
        final var expiredA = enrollWithFleetAndLastSeen("uid-exp-a", "507f1f77bcf86cd799439012", reference.minus(Duration.ofDays(8)));
        final var expiredB = enrollWithFleetAndLastSeen("uid-exp-b", "507f1f77bcf86cd799439012", reference.minus(Duration.ofDays(8)));
        enrollWithFleetAndLastSeen("uid-exp-fresh", "507f1f77bcf86cd799439012", reference);
        capturedEvents.clear();

        clock.setInstant(reference);
        collectorInstanceService.deleteExpired(Duration.ofDays(7));

        final var fingerprints = new ArrayList<String>();
        capturedEvents.forEach(event -> fingerprints.addAll(event.fingerprints()));
        assertThat(fingerprints).containsExactlyInAnyOrder(
                expiredA.activeCertificateFingerprint(), expiredB.activeCertificateFingerprint());
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
        final var issuedCert = new IssuedCertificate(cert.fingerprint(), cert.certificate(), cert.notAfter(), issuerCert.id());

        return collectorInstanceService.enroll(
                instanceUid,
                "000000000000000000000000",
                issuedCert,
                "000000000000000000000000"
        );
    }

    private CollectorInstanceDTO enrollWithFleetAndLastSeen(String instanceUid,
                                                            String fleetId,
                                                            Instant lastSeen) throws Exception {
        final var cert = certBuilder.createEndEntityCert(instanceUid, issuerCert, KeyUsage.digitalSignature, Duration.ofDays(1));
        final var issuedCert = new IssuedCertificate(cert.fingerprint(), cert.certificate(), cert.notAfter(), issuerCert.id());

        final Instant prev = clock.instant();
        clock.setInstant(lastSeen);
        try {
            return collectorInstanceService.enroll(
                    instanceUid,
                    fleetId,
                    issuedCert,
                    "000000000000000000000000"
            );
        } finally {
            clock.setInstant(prev);
        }
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
