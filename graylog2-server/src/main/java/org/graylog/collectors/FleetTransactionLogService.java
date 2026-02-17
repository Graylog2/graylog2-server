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

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.collectors.db.CoalescedActions;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoSequenceService;
import org.graylog2.plugin.system.NodeId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class FleetTransactionLogService {

    static final String COLLECTION_NAME = "fleet_transaction_log";
    static final String SEQUENCE_TOPIC = "fleet_txn_log";
    static final String FIELD_TARGET = "target";
    static final String FIELD_TARGET_ID = "target_id";
    static final String FIELD_TYPE = "type";
    static final String FIELD_PAYLOAD = "payload";
    static final String FIELD_CREATED_AT = "created_at";
    static final String FIELD_CREATED_BY = "created_by";
    /**
     * The maximum number of bulk action targets we allow for a transaction log entry.
     */
    public static final int MAX_BULK_TARGET_SIZE = 100;

    private final MongoCollection<Document> collection;
    private final MongoSequenceService sequenceService;
    private final NodeId nodeId;

    @Inject
    public FleetTransactionLogService(MongoCollections mongoCollections,
                                      MongoSequenceService sequenceService,
                                      NodeId nodeId) {
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, Document.class)
                .withWriteConcern(WriteConcern.JOURNALED);
        this.sequenceService = sequenceService;
        this.nodeId = nodeId;

        // Index for the read path query: (target, target_id, _id)
        collection.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending(FIELD_TARGET, FIELD_TARGET_ID),
                        Indexes.ascending("_id")
                )
        );
    }

    public long appendFleetMarker(String fleetId, MarkerType type) {
        return appendMarker(TransactionMarker.TARGET_FLEET, Set.of(fleetId), type, null);
    }

    public long appendCollectorMarker(Set<String> instanceUids, MarkerType type, @Nullable Document payload) {
        if (instanceUids == null || instanceUids.isEmpty()) {
            throw new IllegalArgumentException("instanceUids must not be empty");
        }
        if (instanceUids.size() > MAX_BULK_TARGET_SIZE) {
            throw new IllegalArgumentException("instanceUids must not exceed " + MAX_BULK_TARGET_SIZE + " elements, got " + instanceUids.size());
        }
        return appendMarker(TransactionMarker.TARGET_COLLECTOR, instanceUids, type, payload);
    }

    private long appendMarker(String target, Set<String> targetIds, MarkerType type, @Nullable Document payload) {
        final long seq = sequenceService.incrementAndGet(SEQUENCE_TOPIC);
        collection.updateOne(
                Filters.eq("_id", seq),
                Updates.combine(
                        Updates.set(FIELD_TARGET, target),
                        Updates.set(FIELD_TARGET_ID, targetIds),
                        Updates.set(FIELD_TYPE, type.name()),
                        Updates.set(FIELD_PAYLOAD, payload),
                        Updates.set(FIELD_CREATED_BY, nodeId.getNodeId()),
                        Updates.currentDate(FIELD_CREATED_AT)
                ),
                new UpdateOptions().upsert(true)
        );
        return seq;
    }

    public List<TransactionMarker> getUnprocessedMarkers(@Nullable String fleetId,
                                                         @Nullable String instanceUid,
                                                         long lastProcessedSeq) {
        if (fleetId == null && instanceUid == null) {
            throw new IllegalArgumentException("At least one of fleetId or instanceUid must be non-null");
        }

        final Bson seqFilter = Filters.gt("_id", lastProcessedSeq);
        final Bson filter;

        if (fleetId != null && instanceUid != null) {
            filter = Filters.and(seqFilter, Filters.or(
                    Filters.and(
                            Filters.eq(FIELD_TARGET, TransactionMarker.TARGET_FLEET),
                            Filters.eq(FIELD_TARGET_ID, fleetId)
                    ),
                    Filters.and(
                            Filters.eq(FIELD_TARGET, TransactionMarker.TARGET_COLLECTOR),
                            Filters.eq(FIELD_TARGET_ID, instanceUid)
                    )
            ));
        } else if (fleetId != null) {
            filter = Filters.and(seqFilter,
                    Filters.eq(FIELD_TARGET, TransactionMarker.TARGET_FLEET),
                    Filters.eq(FIELD_TARGET_ID, fleetId));
        } else {
            filter = Filters.and(seqFilter,
                    Filters.eq(FIELD_TARGET, TransactionMarker.TARGET_COLLECTOR),
                    Filters.eq(FIELD_TARGET_ID, instanceUid));
        }

        return collection.find(filter)
                .sort(new Document("_id", 1))
                .map(this::documentToMarker)
                .into(new ArrayList<>());
    }

    private TransactionMarker documentToMarker(Document doc) {
        final String rawType = doc.getString(FIELD_TYPE);
        return new TransactionMarker(
                doc.getLong("_id"),
                doc.getString(FIELD_TARGET),
                Set.copyOf(doc.getList(FIELD_TARGET_ID, String.class)),
                MarkerType.fromString(rawType),
                rawType,
                doc.get(FIELD_PAYLOAD, Document.class)
        );
    }

    public CoalescedActions coalesce(List<TransactionMarker> markers) {
        return doCoalesce(markers);
    }

    // Package-private static for direct unit testing without MongoDB
    static CoalescedActions doCoalesce(List<TransactionMarker> markers) {
        if (markers.isEmpty()) {
            return CoalescedActions.empty(0L);
        }

        boolean recomputeConfig = false;
        boolean restart = false;
        boolean runDiscovery = false;
        String newFleetId = null;
        long maxSeq = 0L;

        // Check for fleet reassignment first (highest seq wins)
        TransactionMarker latestReassignment = null;
        for (var marker : markers) {
            if (marker.seq() > maxSeq) {
                maxSeq = marker.seq();
            }
            if (marker.type() == MarkerType.FLEET_REASSIGNED) {
                if (latestReassignment == null || marker.seq() > latestReassignment.seq()) {
                    latestReassignment = marker;
                }
            }
        }

        if (latestReassignment != null) {
            // Fleet reassignment: recompute config from new fleet, discard fleet-level markers
            recomputeConfig = true;
            newFleetId = latestReassignment.payload() != null
                    ? latestReassignment.payload().getString("new_fleet_id")
                    : null;

            // Only process collector-level commands (fleet-level ones are from old fleet)
            for (var marker : markers) {
                if (TransactionMarker.TARGET_COLLECTOR.equals(marker.target())) {
                    switch (marker.type()) {
                        case RESTART -> restart = true;
                        case DISCOVERY_RUN -> runDiscovery = true;
                        default -> { /* FLEET_REASSIGNED, UNKNOWN: skip */ }
                    }
                }
            }
        } else {
            // No reassignment: process all markers
            for (var marker : markers) {
                switch (marker.type()) {
                    case CONFIG_CHANGED -> recomputeConfig = true;
                    case RESTART -> restart = true;
                    case DISCOVERY_RUN -> runDiscovery = true;
                    case UNKNOWN -> { }
                    default -> { }
                }
            }
        }

        return new CoalescedActions(recomputeConfig, newFleetId, restart, runDiscovery, maxSeq);
    }

    // Package-private for test access
    MongoCollection<Document> getCollection() {
        return collection;
    }
}
