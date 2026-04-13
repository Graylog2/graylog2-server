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
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.graylog.collectors.db.CoalescedActions;
import org.graylog.collectors.db.FleetReassignedPayload;
import org.graylog.collectors.db.MarkerPayload;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoSequenceService;
import org.graylog2.plugin.system.NodeId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.graylog.collectors.db.TransactionMarker.FIELD_CREATED_AT;
import static org.graylog.collectors.db.TransactionMarker.FIELD_CREATED_BY;
import static org.graylog.collectors.db.TransactionMarker.FIELD_CREATED_BY_USER;
import static org.graylog.collectors.db.TransactionMarker.FIELD_ID;
import static org.graylog.collectors.db.TransactionMarker.FIELD_PAYLOAD;
import static org.graylog.collectors.db.TransactionMarker.FIELD_TARGET;
import static org.graylog.collectors.db.TransactionMarker.FIELD_TARGET_ID;
import static org.graylog.collectors.db.TransactionMarker.FIELD_TYPE;
import static org.graylog.collectors.db.TransactionMarker.TARGET_COLLECTOR;
import static org.graylog.collectors.db.TransactionMarker.TARGET_FLEET;

@Singleton
public class FleetTransactionLogService {

    static final String COLLECTION_NAME = "collector_fleet_transaction_log";
    static final String SEQUENCE_TOPIC = "fleet_txn_log";

    /**
     * The maximum number of bulk action targets we allow for a transaction log entry.
     */
    public static final int MAX_BULK_TARGET_SIZE = 100;

    private final MongoCollection<TransactionMarker> collection;
    private final MongoSequenceService sequenceService;
    private final NodeId nodeId;

    @Inject
    public FleetTransactionLogService(MongoCollections mongoCollections,
                                      MongoSequenceService sequenceService,
                                      NodeId nodeId) {
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, TransactionMarker.class)
                .withWriteConcern(WriteConcern.JOURNALED);
        this.sequenceService = sequenceService;
        this.nodeId = nodeId;

        // Index for the read path query: (target, target_id, _id)
        collection.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending(FIELD_TARGET, FIELD_TARGET_ID),
                        Indexes.ascending(FIELD_ID)
                )
        );
        collection.createIndex(Indexes.ascending(FIELD_CREATED_AT));
    }

    public long appendFleetMarker(String fleetId, MarkerType type) {
        return appendFleetMarker(Set.of(fleetId), type);

    }

    public long appendFleetMarker(Set<String> fleetIds, MarkerType type) {
        return appendMarker(TARGET_FLEET, fleetIds, type, null);
    }

    public long appendCollectorMarker(Set<String> instanceUids, MarkerType type, @Nullable MarkerPayload payload) {
        if (instanceUids == null || instanceUids.isEmpty()) {
            throw new IllegalArgumentException("instanceUids must not be empty");
        }
        if (instanceUids.size() > MAX_BULK_TARGET_SIZE) {
            throw new IllegalArgumentException("instanceUids must not exceed " + MAX_BULK_TARGET_SIZE + " elements, got " + instanceUids.size());
        }
        return appendMarker(TARGET_COLLECTOR, instanceUids, type, payload);
    }

    private long appendMarker(String target, Set<String> targetIds, MarkerType type, @Nullable MarkerPayload payload) {
        if (targetIds == null || targetIds.isEmpty() || targetIds.stream().noneMatch(StringUtils::isNotBlank)) {
            throw new IllegalArgumentException("targetIds must not be empty");
        }

        final long seq = sequenceService.incrementAndGet(SEQUENCE_TOPIC);
        collection.updateOne(
                Filters.eq(FIELD_ID, seq),
                Updates.combine(
                        Updates.set(FIELD_TARGET, target),
                        Updates.set(FIELD_TARGET_ID, targetIds),
                        Updates.set(FIELD_TYPE, type.name()),
                        Updates.set(FIELD_PAYLOAD, payload),
                        Updates.set(FIELD_CREATED_BY, nodeId.getNodeId()),
                        Updates.currentDate(FIELD_CREATED_AT),
                        Updates.set(FIELD_CREATED_BY_USER, resolveCurrentUsername())
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

        final Bson seqFilter = Filters.gt(FIELD_ID, lastProcessedSeq);
        final Bson filter;

        if (fleetId != null && instanceUid != null) {
            filter = Filters.and(seqFilter, Filters.or(
                    Filters.and(
                            Filters.eq(FIELD_TARGET, TARGET_FLEET),
                            Filters.eq(FIELD_TARGET_ID, fleetId)
                    ),
                    Filters.and(
                            Filters.eq(FIELD_TARGET, TARGET_COLLECTOR),
                            Filters.eq(FIELD_TARGET_ID, instanceUid)
                    )
            ));
        } else if (fleetId != null) {
            filter = Filters.and(seqFilter,
                    Filters.eq(FIELD_TARGET, TARGET_FLEET),
                    Filters.eq(FIELD_TARGET_ID, fleetId));
        } else {
            filter = Filters.and(seqFilter,
                    Filters.eq(FIELD_TARGET, TARGET_COLLECTOR),
                    Filters.eq(FIELD_TARGET_ID, instanceUid));
        }

        return collection.find(filter)
                .sort(Sorts.ascending(FIELD_ID))
                .into(new ArrayList<>());
    }

    @Nullable
    private static String resolveCurrentUsername() {
        try {
            final var subject = org.apache.shiro.SecurityUtils.getSubject();
            if (subject != null && subject.getPrincipal() != null) {
                return subject.getPrincipal().toString();
            }
        } catch (Exception e) {
            // No Shiro subject available (non-HTTP context) — this is expected
        }
        return null;
    }

    public List<TransactionMarker> getRecentMarkers(int limit) {
        // Exclude UNKNOWN by filtering for known types only
        final Bson filter = Filters.in(FIELD_TYPE,
                MarkerType.CONFIG_CHANGED.name(),
                MarkerType.INGEST_CONFIG_CHANGED.name(),
                MarkerType.RESTART.name(),
                MarkerType.DISCOVERY_RUN.name(),
                MarkerType.FLEET_REASSIGNED.name());

        return collection.find(filter)
                .sort(Sorts.descending(FIELD_ID))
                .limit(limit)
                .into(new ArrayList<>());
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
        boolean recomputeIngestConfig = false;
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
            newFleetId = latestReassignment.payload() instanceof FleetReassignedPayload(String fleetId)
                    ? fleetId
                    : null;

            // Only process collector-level commands (fleet-level ones are from old fleet)
            for (var marker : markers) {
                if (TARGET_COLLECTOR.equals(marker.target())) {
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
                    case INGEST_CONFIG_CHANGED -> recomputeIngestConfig = true;
                    case RESTART -> restart = true;
                    case DISCOVERY_RUN -> runDiscovery = true;
                    case UNKNOWN -> {
                    }
                    default -> {
                    }
                }
            }
        }

        return new CoalescedActions(recomputeConfig, recomputeIngestConfig, newFleetId, restart, runDiscovery, maxSeq);
    }

    public long countMarkersSince(Instant since) {
        return collection.countDocuments(Filters.gte(FIELD_CREATED_AT, since));
    }

    // Package-private for test access
    MongoCollection<TransactionMarker> getCollection() {
        return collection;
    }
}
