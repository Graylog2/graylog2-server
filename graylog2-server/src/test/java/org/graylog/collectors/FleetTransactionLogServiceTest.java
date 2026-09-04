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
package org.graylog.collectors;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.FleetReassignedPayload;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoSequenceService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class FleetTransactionLogServiceTest {

    private static final NodeId NODE_ID = new SimpleNodeId("test-node-1");

    private FleetTransactionLogService service;
    private MongoCollection<Document> rawCollection;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        var sequenceService = new MongoSequenceService(
                mongoCollections,
                NODE_ID,
                Set.of(FleetTransactionLogService.SEQUENCE_TOPIC)
        );
        service = new FleetTransactionLogService(mongoCollections, sequenceService, NODE_ID);
        rawCollection = mongoCollections.nonEntityCollection(FleetTransactionLogService.COLLECTION_NAME, Document.class);
    }

    // --- Write path tests ---

    @Test
    void appendFleetMarkerStoresCorrectDocument() {
        long seq = service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);

        assertThat(seq).isEqualTo(1L);

        var marker = service.getCollection().find(Filters.eq("_id", 1L)).first();
        assertThat(marker).isNotNull();
        assertThat(marker.target()).isEqualTo("fleet");
        assertThat(marker.targetIds()).containsExactly("fleet-1");
        assertThat(marker.type()).isEqualTo(MarkerType.CONFIG_CHANGED);
        assertThat(marker.payload()).isNull();
        assertThat(marker.createdAt()).isNotNull();
        assertThat(marker.createdByUser()).isNull();
    }

    @Test
    void appendFleetMarkerDoesntAllowEmptyTarget() {
        assertThatThrownBy(() -> service.appendFleetMarker("", MarkerType.CONFIG_CHANGED))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.appendFleetMarker(Set.of(""), MarkerType.CONFIG_CHANGED))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.appendFleetMarker(Set.of(), MarkerType.CONFIG_CHANGED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void appendCollectorMarkerWithPayloadStoresPayload() {
        var payload = new FleetReassignedPayload("fleet-B");
        long seq = service.appendCollectorMarker(Set.of("inst-1"), MarkerType.FLEET_REASSIGNED, payload);

        assertThat(seq).isEqualTo(1L);

        var marker = service.getCollection().find(Filters.eq("_id", 1L)).first();
        assertThat(marker).isNotNull();
        assertThat(marker.target()).isEqualTo("collector");
        assertThat(marker.targetIds()).containsExactly("inst-1");
        assertThat(marker.type()).isEqualTo(MarkerType.FLEET_REASSIGNED);
        assertThat(marker.payload()).isInstanceOf(FleetReassignedPayload.class);
        assertThat(((FleetReassignedPayload) marker.payload()).newFleetId()).isEqualTo("fleet-B");
    }

    @Test
    void sequenceNumbersAreMonotonicallyIncreasing() {
        long seq1 = service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);
        long seq2 = service.appendCollectorMarker(Set.of("inst-1"), MarkerType.RESTART, null);
        long seq3 = service.appendFleetMarker("fleet-2", MarkerType.CONFIG_CHANGED);

        assertThat(seq1).isEqualTo(1L);
        assertThat(seq2).isEqualTo(2L);
        assertThat(seq3).isEqualTo(3L);
    }

    // --- Read path tests ---

    @Test
    void getUnprocessedMarkersReturnsByFleetId() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);
        service.appendFleetMarker("fleet-2", MarkerType.CONFIG_CHANGED);

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 0L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().targetIds()).containsExactly("fleet-1");
    }

    @Test
    void getUnprocessedMarkersReturnsByInstanceUid() {
        service.appendCollectorMarker(Set.of("inst-1"), MarkerType.RESTART, null);
        service.appendCollectorMarker(Set.of("inst-2"), MarkerType.RESTART, null);

        List<TransactionMarker> markers = service.getUnprocessedMarkers(null, "inst-1", 0L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().targetIds()).containsExactly("inst-1");
    }

    @Test
    void getUnprocessedMarkersReturnsBothScopesWithOr() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);
        service.appendCollectorMarker(Set.of("inst-1"), MarkerType.RESTART, null);
        service.appendFleetMarker("fleet-2", MarkerType.CONFIG_CHANGED);     // different fleet
        service.appendCollectorMarker(Set.of("inst-2"), MarkerType.RESTART, null);   // different collector

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", "inst-1", 0L);

        assertThat(markers).hasSize(2);
    }

    @Test
    void getUnprocessedMarkersFiltersByLastProcessedSeq() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 1
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 2
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 3

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 2L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().seq()).isEqualTo(3L);
    }

    @Test
    void getUnprocessedMarkersThrowsWhenBothNull() {
        assertThatThrownBy(() -> service.getUnprocessedMarkers(null, null, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUnprocessedMarkersReturnsEmptyWhenNoneMatch() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 1

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 1L);

        assertThat(markers).isEmpty();
    }

    @Test
    void getUnprocessedMarkersDeserializesUnknownTypesAsUnknown() {
        // Insert a marker with an unknown type directly into the raw collection
        rawCollection.insertOne(new Document("_id", 999L)
                .append("target", "fleet")
                .append("target_id", List.of("fleet-1"))
                .append("type", "FUTURE_MARKER_TYPE")
                .append("created_by", "test"));

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 0L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().type()).isEqualTo(MarkerType.UNKNOWN);
    }

    // --- Recent markers tests ---

    @Test
    void getRecentMarkersReturnsLastNDescending() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);       // seq 1
        service.appendCollectorMarker(Set.of("inst-1"), MarkerType.RESTART, null); // seq 2
        service.appendFleetMarker("fleet-2", MarkerType.DISCOVERY_RUN);        // seq 3

        List<TransactionMarker> markers = service.getRecentMarkers(2);

        assertThat(markers).hasSize(2);
        assertThat(markers.get(0).seq()).isEqualTo(3L); // most recent first
        assertThat(markers.get(1).seq()).isEqualTo(2L);
    }

    @Test
    void getRecentMarkersExcludesUnknownType() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 1

        // Insert an UNKNOWN marker directly via raw collection
        rawCollection.insertOne(new Document("_id", 999L)
                .append("target", "fleet")
                .append("target_id", List.of("fleet-1"))
                .append("type", "FUTURE_MARKER_TYPE")
                .append("created_by", "test"));

        List<TransactionMarker> markers = service.getRecentMarkers(10);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().type()).isEqualTo(MarkerType.CONFIG_CHANGED);
    }

    // --- pendingChangesLookup / isPending tests ---

    @Test
    void pendingChangesLookupCoversFleetAndCollectorScopes() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);              // seq 1
        service.appendCollectorMarker(Set.of("uid-C"), MarkerType.RESTART, null);     // seq 2

        final var lookup = service.pendingChangesLookup();

        assertThat(lookup.isPending(instance("uid-A", "fleet-1", 0L))).isTrue();   // pending via its fleet's marker
        assertThat(lookup.isPending(instance("uid-B", "fleet-2", 0L))).isFalse();  // nothing targets fleet-2 or uid-B
        assertThat(lookup.isPending(instance("uid-C", "fleet-2", 0L))).isTrue();   // pending via its own marker
    }

    @Test
    void pendingChangesLookupHonorsEachInstancesOwnCursor() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);              // seq 1
        long s2 = service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);    // seq 2

        final var lookup = service.pendingChangesLookup();

        // same fleet, different applied cursors: only the lagging instance is pending
        assertThat(lookup.isPending(instance("uid-A", "fleet-1", 0L))).isTrue();
        assertThat(lookup.isPending(instance("uid-B", "fleet-1", s2))).isFalse();
    }

    @Test
    void pendingChangesLookupTreatsMarkerAtCursorAsApplied() {
        long s1 = service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);

        final var lookup = service.pendingChangesLookup();

        assertThat(lookup.isPending(instance("uid-A", "fleet-1", s1))).isFalse();
    }

    @Test
    void pendingChangesLookupUnwindsBulkCollectorMarkers() {
        service.appendCollectorMarker(Set.of("uid-A", "uid-B"), MarkerType.RESTART, null);

        final var lookup = service.pendingChangesLookup();

        assertThat(lookup.isPending(instance("uid-A", "fleet-1", 0L))).isTrue();
        assertThat(lookup.isPending(instance("uid-B", "fleet-1", 0L))).isTrue();
        assertThat(lookup.isPending(instance("uid-C", "fleet-1", 0L))).isFalse();
    }

    @Test
    void pendingChangesLookupRecordsEachFleetFromBulkFleetMarkers() {
        // a bulk marker targets both fleets; each fleet gets its own max-seq entry
        service.appendFleetMarker(Set.of("fleet-1", "fleet-2"), MarkerType.CONFIG_CHANGED);

        final var lookup = service.pendingChangesLookup();

        assertThat(lookup.maxByFleetId()).containsOnlyKeys("fleet-1", "fleet-2");
        assertThat(lookup.isPending(instance("uid-A", "fleet-1", 0L))).isTrue();
        assertThat(lookup.isPending(instance("uid-B", "fleet-2", 0L))).isTrue();
        assertThat(lookup.isPending(instance("uid-C", "fleet-3", 0L))).isFalse();
    }

    @Test
    void pendingChangesLookupIsEmptyWhenNoMarkers() {
        final var lookup = service.pendingChangesLookup();

        assertThat(lookup.maxByFleetId()).isEmpty();
        assertThat(lookup.maxByInstanceUid()).isEmpty();
        assertThat(lookup.isPending(instance("uid-A", "fleet-1", 0L))).isFalse();
    }

    // --- purgeMarkers / highestPurgedSeq tests ---

    @Test
    void highestPurgedSeqIsZeroOnEmptyLog() {
        assertThat(service.highestPurgedSeq()).isZero();
    }

    @Test
    void highestPurgedSeqIsZeroWhenNothingWasPurged() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 1
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 2

        assertThat(service.highestPurgedSeq()).isZero();
    }

    @Test
    void purgeMarkersDeletesAllAgedMarkersWhenEnoughYoungMarkersRemain() {
        appendMarkers(6);
        backdate(4, Instant.now().minus(Duration.ofDays(10))); // seqs 1-4 aged, 5-6 young

        final long deleted = service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 2);

        assertThat(deleted).isEqualTo(4L); // the two young markers already satisfy numToKeep
        assertThat(service.highestPurgedSeq()).isEqualTo(4L);
        assertThat(rawCollection.countDocuments()).isEqualTo(2L);
    }

    @Test
    void purgeMarkersRetainsNewestAgedMarkersToReachNumToKeep() {
        appendMarkers(6);
        backdate(4, Instant.now().minus(Duration.ofDays(10))); // seqs 1-4 aged, 5-6 young

        final long deleted = service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 4);

        assertThat(deleted).isEqualTo(2L); // aged seqs 3-4 fill up the total of 4
        assertThat(service.highestPurgedSeq()).isEqualTo(2L);
        assertThat(rawCollection.countDocuments()).isEqualTo(4L);
    }

    @Test
    void purgeMarkersDoesNothingWhenTotalDoesNotExceedNumToKeep() {
        appendMarkers(3);
        backdate(3, Instant.now().minus(Duration.ofDays(10)));

        final long deleted = service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 3);

        assertThat(deleted).isZero();
        assertThat(service.highestPurgedSeq()).isZero();
    }

    @Test
    void purgeMarkersDoesNothingWhenAllMarkersAreYoung() {
        appendMarkers(3);

        final long deleted = service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 1);

        assertThat(deleted).isZero();
        assertThat(rawCollection.countDocuments()).isEqualTo(3L);
    }

    @Test
    void purgeMarkersDeletesAllAgedMarkersDespiteInvertedTimestamps() {
        appendMarkers(4);
        // created_at inverted relative to seq among the aged markers: seq 2 is the oldest by time
        backdate(1, 1, Instant.now().minus(Duration.ofDays(35)));
        backdate(2, 2, Instant.now().minus(Duration.ofDays(40)));
        backdate(3, 3, Instant.now().minus(Duration.ofDays(38)));
        // seq 4 stays young

        final long deleted = service.purgeMarkers(Instant.now().minus(Duration.ofDays(30)), 1);

        // the boundary is the highest aged seq (3), so no aged marker survives below a deleted one
        assertThat(deleted).isEqualTo(3L);
        assertThat(service.highestPurgedSeq()).isEqualTo(3L);
    }

    @Test
    void purgeMarkersIsUnaffectedBySequenceHoles() {
        // seqs 3 and 4 were issued but their markers never got written (failed appends)
        for (long seq : new long[]{1L, 2L, 5L, 6L}) {
            rawCollection.insertOne(new Document("_id", seq)
                    .append("target", "fleet")
                    .append("target_id", List.of("fleet-1"))
                    .append("type", "CONFIG_CHANGED")
                    .append("created_by", "test")
                    .append("created_at", new Date()));
        }
        backdate(5, Instant.now().minus(Duration.ofDays(10))); // seqs 1, 2, 5 aged; 6 young

        final long deleted = service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 2);

        assertThat(deleted).isEqualTo(2L); // seqs 1-2; seq 5 retained to reach numToKeep
        assertThat(service.highestPurgedSeq()).isEqualTo(4L);
    }

    @Test
    void purgeMarkersNeverDeletesMarkersYoungerThanCutoff() {
        appendMarkers(4);
        // timestamp inversion across the cutoff: seq 3 is aged but follows the young seq 2
        backdate(1, 1, Instant.now().minus(Duration.ofDays(40)));
        // seq 2 stays young
        backdate(3, 3, Instant.now().minus(Duration.ofDays(38)));
        // seq 4 stays young

        final long deleted = service.purgeMarkers(Instant.now().minus(Duration.ofDays(30)), 1);

        // deletion stops below the first young marker (seq 2); the aged seq 3 survives this
        // run and is purged later, once the advancing cutoff passes seq 2
        assertThat(deleted).isEqualTo(1L);
        assertThat(service.highestPurgedSeq()).isEqualTo(1L);
    }

    @Test
    void purgeMarkersRejectsNonPositiveNumToKeep() {
        assertThatThrownBy(() -> service.purgeMarkers(Instant.now(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.purgeMarkers(Instant.now(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void purgeMarkersIsIdempotent() {
        appendMarkers(6);
        backdate(4, Instant.now().minus(Duration.ofDays(10)));

        final var cutoff = Instant.now().minus(Duration.ofDays(1));
        service.purgeMarkers(cutoff, 2);
        final long deletedOnSecondRun = service.purgeMarkers(cutoff, 2);

        assertThat(deletedOnSecondRun).isZero();
        assertThat(service.highestPurgedSeq()).isEqualTo(4L);
    }

    // --- truncation-aware pending / coalesce tests ---

    @Test
    void hasPendingChangesConsidersCursorsBelowHighestPurgedSeq() {
        appendMarkers(6);
        backdate(4, Instant.now().minus(Duration.ofDays(10)));
        service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 2); // purges seqs 1-4

        // fleet-2 has no retained markers: pending is decided by the purge state alone
        assertThat(service.hasPendingChanges("fleet-2", null, 3L)).isTrue();
        assertThat(service.hasPendingChanges("fleet-2", null, 4L)).isFalse(); // saw everything purged
        // fleet-1 still has retained unprocessed markers (seqs 5-6)
        assertThat(service.hasPendingChanges("fleet-1", null, 4L)).isTrue();
        assertThat(service.hasPendingChanges("fleet-1", null, 6L)).isFalse();
    }

    @Test
    void coalesceForcesRecomputeForCursorsBelowHighestPurgedSeq() {
        appendMarkers(6);
        backdate(4, Instant.now().minus(Duration.ofDays(10)));
        service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 2); // purges seqs 1-4

        final var forced = service.coalesce(List.of(), 3L);
        assertThat(forced.recomputeConfig()).isTrue();
        assertThat(forced.recomputeIngestConfig()).isTrue();
        assertThat(forced.maxSeq()).isEqualTo(4L); // acknowledges the purged range

        final var atBoundary = service.coalesce(List.of(), 4L);
        assertThat(atBoundary.recomputeConfig()).isFalse();
        assertThat(atBoundary.recomputeIngestConfig()).isFalse();
    }

    @Test
    void coalesceDoesNotForceRecomputeWhenNothingWasPurged() {
        appendMarkers(2);

        final var coalesced = service.coalesce(List.of(), 0L);

        assertThat(coalesced.recomputeConfig()).isFalse();
        assertThat(coalesced.recomputeIngestConfig()).isFalse();
    }

    @Test
    void pendingChangesLookupCarriesHighestPurgedSeq() {
        appendMarkers(6);
        backdate(4, Instant.now().minus(Duration.ofDays(10)));
        service.purgeMarkers(Instant.now().minus(Duration.ofDays(1)), 2); // purges seqs 1-4

        final var lookup = service.pendingChangesLookup();

        assertThat(lookup.highestPurgedSeq()).isEqualTo(4L);
        // an instance in a fleet without retained markers is pending purely due to truncation
        assertThat(lookup.isPending(instance("uid-A", "fleet-2", 3L))).isTrue();
        assertThat(lookup.isPending(instance("uid-A", "fleet-2", 4L))).isFalse();
    }

    private void appendMarkers(int count) {
        for (int i = 0; i < count; i++) {
            service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);
        }
    }

    private void backdate(long upToSeq, Instant createdAt) {
        backdate(1, upToSeq, createdAt);
    }

    private void backdate(long fromSeq, long toSeq, Instant createdAt) {
        rawCollection.updateMany(
                Filters.and(Filters.gte("_id", fromSeq), Filters.lte("_id", toSeq)),
                new Document("$set", new Document("created_at", Date.from(createdAt))));
    }

    private static CollectorInstanceDTO instance(String uid, String fleetId, long lastProcessedTxnSeq) {
        return CollectorInstanceDTO.builder()
                .instanceUid(uid)
                .fleetId(fleetId)
                .lastProcessedTxnSeq(lastProcessedTxnSeq)
                .lastSeen(Instant.EPOCH)
                .enrolledAt(Instant.EPOCH)
                .messageSeqNum(0L)
                .capabilities(0L)
                .activeCertificateFingerprint("fp-" + uid)
                .activeCertificatePem("pem")
                .activeCertificateExpiresAt(Instant.EPOCH)
                .issuingCaId("ca-1")
                .enrollmentTokenId("token-1")
                .build();
    }
}
