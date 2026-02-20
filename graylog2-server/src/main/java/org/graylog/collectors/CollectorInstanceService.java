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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.InsertOneResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.search.SearchQuery;
import org.mongojack.Id;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.setOnInsert;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_CAPABILITIES;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_CERTIFICATE_FINGERPRINT;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_FLEET_ID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_IDENTIFYING_ATTRIBUTES;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_INSTANCE_UID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_LAST_PROCESSED_TXN_SEQ;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_LAST_SEEN;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_MESSAGE_SEQ_NUM;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES;
import static org.graylog2.database.MongoEntity.FIELD_ID;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

@Singleton
public class CollectorInstanceService {

    private final MongoCollection<CollectorInstanceDTO> collection;
    private final MongoPaginationHelper<CollectorInstanceDTO> paginationHelper;
    private final com.mongodb.client.MongoCollection<MinimalCollectorInstanceDTO> projectedCollection;

    @Inject
    public CollectorInstanceService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection("collector_instances", CollectorInstanceDTO.class);
        projectedCollection = mongoCollections.nonEntityCollection("collector_instances", MinimalCollectorInstanceDTO.class);
        paginationHelper = mongoCollections.paginationHelper(collection);

        collection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(FIELD_INSTANCE_UID), new IndexOptions().unique(true)),
                new IndexModel(Indexes.ascending(FIELD_IDENTIFYING_ATTRIBUTES + ".key",
                        FIELD_IDENTIFYING_ATTRIBUTES + ".value")),
                new IndexModel(Indexes.ascending(FIELD_NON_IDENTIFYING_ATTRIBUTES + ".key",
                        FIELD_NON_IDENTIFYING_ATTRIBUTES + ".value")),
                new IndexModel(Indexes.ascending(FIELD_CERTIFICATE_FINGERPRINT), new IndexOptions().unique(true))
        ));
    }

    /**
     * Saves an incoming collector instance report and returns the previously saved state if available.
     *
     * @param update the report to save
     * @return optionally the previous version of the report
     */
    public Optional<MinimalCollectorInstanceDTO> createOrUpdateFromReport(CollectorInstanceReport update) {
        final List<Bson> updateOps = new ArrayList<>();

        updateOps.add(setOnInsert(FIELD_INSTANCE_UID, update.instanceUid()));
        updateOps.add(set(FIELD_LAST_SEEN, update.lastSeen()));
        updateOps.add(set(FIELD_MESSAGE_SEQ_NUM, update.messageSeqNum()));
        updateOps.add(set(FIELD_CAPABILITIES, update.capabilities()));
        if (update.identifyingAttributes().isPresent()) {
            updateOps.add(set(FIELD_IDENTIFYING_ATTRIBUTES, update.identifyingAttributes().get()));
        }
        if (update.nonIdentifyingAttributes().isPresent()) {
            updateOps.add(set(FIELD_NON_IDENTIFYING_ATTRIBUTES, update.nonIdentifyingAttributes().get()));
        }

        // we request the ReturnDocument.BEFORE here to avoid having to load the previous document in full just
        // to retrieve the previous `message_seq_num`, which we need to determine what to do next.
        // the result is not the full CollectorInstanceDTO as we have it, but the minimal set of fields necessary to
        // determine next steps
        final MinimalCollectorInstanceDTO previousInstanceDto = projectedCollection.findOneAndUpdate(Filters.eq(FIELD_INSTANCE_UID, update.instanceUid()),
                combine(updateOps),
                new FindOneAndUpdateOptions()
                        .returnDocument(ReturnDocument.BEFORE)
                        // TODO we should define a separate minimal "DTO" for using projections, otherwise we have too many optionals
                        .projection(Projections.include(FIELD_MESSAGE_SEQ_NUM, FIELD_LAST_PROCESSED_TXN_SEQ, FIELD_FLEET_ID))
                        .upsert(true));

        return Optional.ofNullable(previousInstanceDto);
    }

    public boolean existsByInstanceUid(String instanceUid) {
        return collection.countDocuments(Filters.eq(FIELD_INSTANCE_UID, instanceUid)) == 1L;
    }

    public CollectorInstanceDTO enroll(String instanceUid, String fleetId, String fingerprint, String certPem, String caId, Instant enrolledAt) {
        final CollectorInstanceDTO dto = CollectorInstanceDTO.builder()
                .instanceUid(instanceUid)
                .lastSeen(enrolledAt)
                .messageSeqNum(0L) // set to 0 explicitly for clarity, this will cause full resync later in the connection
                .capabilities(0L) // capabilities are unspecified at this point
                .fleetId(fleetId)
                .certificateFingerprint(fingerprint)
                .certificatePem(certPem)
                .issuingCaId(caId)
                .enrolledAt(enrolledAt)
                .build();
        final InsertOneResult insertOneResult = collection.insertOne(dto);
        return dto.toBuilder()
                .id(insertedIdAsString(insertOneResult))
                .build();
    }

    public Optional<CollectorInstanceDTO> findByFingerprint(String fingerprint) {
        return Optional.ofNullable(
                collection.find(Filters.eq(FIELD_CERTIFICATE_FINGERPRINT, fingerprint)).first()
        );
    }

    public Optional<CollectorInstanceDTO> findByInstanceUid(String instanceUid) {
        return Optional.ofNullable(
                collection.find(Filters.eq(FIELD_INSTANCE_UID, instanceUid)).first()
        );
    }

    public PaginatedList<CollectorInstanceDTO> findPaginated(SearchQuery searchQuery, Bson sort, int page, int perPage) {
        return paginationHelper.filter(searchQuery.toBson()).sort(sort).perPage(perPage).page(page);
    }

    public long count() {
        return collection.countDocuments();
    }

    public long countOnline(Instant onlineThreshold) {
        return collection.countDocuments(Filters.gte(FIELD_LAST_SEEN, onlineThreshold));
    }

    public long countByFleet(String fleetId) {
        return collection.countDocuments(Filters.eq(CollectorInstanceDTO.FIELD_FLEET_ID, fleetId));
    }

    public long countOnlineByFleet(String fleetId, Instant onlineThreshold) {
        return collection.countDocuments(Filters.and(
                Filters.eq(CollectorInstanceDTO.FIELD_FLEET_ID, fleetId),
                Filters.gte(FIELD_LAST_SEEN, onlineThreshold)
        ));
    }

    public record MinimalCollectorInstanceDTO(@Id @JsonProperty(FIELD_ID) String id,
                                              @JsonProperty(FIELD_FLEET_ID) String fleetId,
                                              @JsonProperty(FIELD_MESSAGE_SEQ_NUM) long messageSeqNum,
                                              @JsonProperty(FIELD_LAST_PROCESSED_TXN_SEQ) long lastProcessTxnSeq) {}
}
