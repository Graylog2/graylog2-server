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
import com.google.common.base.Predicates;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.InsertOneResult;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.collectors.opamp.IssuedCertificate;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.mongojack.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_ACTIVE_CERTIFICATE_EXPIRES_AT;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_ACTIVE_CERTIFICATE_FINGERPRINT;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_ACTIVE_CERTIFICATE_PEM;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_CAPABILITIES;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_ENROLLMENT_TOKEN_ID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_FLEET_ID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_IDENTIFYING_ATTRIBUTES;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_INSTANCE_UID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_ISSUING_CA_ID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_LAST_PROCESSED_TXN_SEQ;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_LAST_SEEN;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_MESSAGE_SEQ_NUM;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_EXPIRES_AT;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_FINGERPRINT;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_NEXT_CERTIFICATE_PEM;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES;
import static org.graylog2.database.MongoEntity.FIELD_ID;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class CollectorInstanceService {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorInstanceService.class);
    private static final String OS_TYPE_KEY = "os.type";

    private final MongoCollection<CollectorInstanceDTO> collection;
    private final MongoPaginationHelper<CollectorInstanceDTO> paginationHelper;
    private final com.mongodb.client.MongoCollection<MinimalCollectorInstanceDTO> projectedCollection;
    private final Clock clock;

    @Inject
    public CollectorInstanceService(MongoCollections mongoCollections, Clock clock) {
        collection = mongoCollections.collection("collector_instances", CollectorInstanceDTO.class);
        projectedCollection = mongoCollections.nonEntityCollection("collector_instances", MinimalCollectorInstanceDTO.class);
        paginationHelper = mongoCollections.paginationHelper(collection);
        this.clock = clock;

        try {
            collection.createIndexes(List.of(
                    new IndexModel(Indexes.ascending(FIELD_INSTANCE_UID), new IndexOptions().unique(true)),
                    new IndexModel(Indexes.ascending(FIELD_IDENTIFYING_ATTRIBUTES + ".key",
                            FIELD_IDENTIFYING_ATTRIBUTES + ".value")),
                    new IndexModel(Indexes.ascending(FIELD_NON_IDENTIFYING_ATTRIBUTES + ".key",
                            FIELD_NON_IDENTIFYING_ATTRIBUTES + ".value")),
                    new IndexModel(Indexes.ascending(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT), new IndexOptions().unique(true)),
                    new IndexModel(Indexes.ascending(FIELD_LAST_SEEN))
            ));
        } catch (Exception e) {
            LOG.error("Database index creation failed", e);
        }
    }

    /**
     * Saves an incoming collector instance report and returns the previously saved state. Throws an exception when
     * the instance is not enrolled.
     *
     * @param update the instance report to save
     * @return the previous saved state
     * @throws IllegalArgumentException when the instance is not enrolled
     */
    public MinimalCollectorInstanceDTO updateFromReport(CollectorInstanceReport update) {
        final List<Bson> updateOps = new ArrayList<>();

        updateOps.add(set(FIELD_LAST_SEEN, Date.from(update.lastSeen())));
        updateOps.add(set(FIELD_MESSAGE_SEQ_NUM, update.messageSeqNum()));
        updateOps.add(set(FIELD_CAPABILITIES, update.capabilities()));
        if (update.lastProcessedTxnSeq().isPresent()) {
            updateOps.add(set(FIELD_LAST_PROCESSED_TXN_SEQ, update.lastProcessedTxnSeq().getAsLong()));
        }
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
        final var previousInstanceDto = projectedCollection.findOneAndUpdate(Filters.eq(FIELD_INSTANCE_UID, update.instanceUid()),
                combine(updateOps),
                new FindOneAndUpdateOptions()
                        .returnDocument(ReturnDocument.BEFORE)
                        .projection(Projections.fields(
                                Projections.include(FIELD_MESSAGE_SEQ_NUM, FIELD_LAST_PROCESSED_TXN_SEQ, FIELD_FLEET_ID),
                                Projections.elemMatch(FIELD_NON_IDENTIFYING_ATTRIBUTES, Filters.eq(Attribute.FIELD_KEY, OS_TYPE_KEY))
                        )));

        if (previousInstanceDto == null) {
            // If there was no existing document, the instance was not enrolled.
            throw new IllegalArgumentException("Instance not enrolled: " + update.instanceUid());
        }

        return previousInstanceDto;
    }

    /**
     * Updates an existing collector instance to a new fleet id.
     *
     * @param instanceUid the instance to update
     * @param newFleetId  the new fleet id to save
     */
    public void updateCurrentFleet(@Nonnull String instanceUid, @Nonnull String newFleetId) {
        collection.updateOne(Filters.eq(FIELD_INSTANCE_UID, instanceUid), set(FIELD_FLEET_ID, newFleetId));
    }

    /**
     * Inserts a new collector instance record for a first-time enrollment.
     * <p>
     * The {@code instance_uid} index is unique; concurrent first-time enrollments for the same UID
     * surface as {@link com.mongodb.MongoWriteException} with a duplicate-key error.
     *
     * @param instanceUid       the agent's self-chosen OpAMP instance UID
     * @param fleetId           the fleet the enrolling token belongs to
     * @param issuedCert        the freshly signed agent certificate
     * @param enrollmentTokenId the id of the enrollment token that authorized this enrollment
     * @return the inserted DTO, populated with the generated Mongo {@code _id}
     */
    public CollectorInstanceDTO enroll(String instanceUid,
                                       String fleetId,
                                       IssuedCertificate issuedCert,
                                       String enrollmentTokenId) {

        final var now = clock.instant();

        final CollectorInstanceDTO dto = CollectorInstanceDTO.builder()
                .instanceUid(instanceUid)
                .lastSeen(now)
                .messageSeqNum(0L) // set to 0 explicitly for clarity, this will cause full resync later in the connection
                .capabilities(0L) // capabilities are unspecified at this point
                .fleetId(fleetId)
                .activeCertificateFingerprint(issuedCert.fingerprint())
                .activeCertificatePem(issuedCert.certPem())
                .activeCertificateExpiresAt(issuedCert.notAfter())
                .issuingCaId(issuedCert.issuerId())
                .enrolledAt(now)
                .enrollmentTokenId(enrollmentTokenId)
                .build();
        final InsertOneResult insertOneResult = collection.insertOne(dto);
        return dto.toBuilder()
                .id(insertedIdAsString(insertOneResult))
                .build();
    }

    /**
     * Atomically re-issues a collector's active certificate on an existing record.
     * <p>
     * The update is a compare-and-swap: it only matches the record with the given Mongo
     * {@code _id} <em>and</em> the given active certificate fingerprint. Callers must pass the id
     * and fingerprint they observed when they verified the re-enrollment request (typically via
     * {@link #findByInstanceUid}). This ensures the update only lands on the exact state the
     * caller validated. If the record has since been deleted, replaced, or had its active
     * certificate changed (e.g. by a concurrent renewal activation), the update matches nothing
     * and this method throws.
     * <p>
     * The caller (typically {@code OpAmpService.handleEnrollment}) is responsible for enforcing
     * proof-of-possession (matching the CSR public key against the stored active certificate's
     * public key) before calling this method. This service method performs no such check — it
     * only enforces the compare-and-swap race guard.
     *
     * @param id                        the Mongo {@code _id} of the record to update
     * @param expectedActiveFingerprint the active certificate fingerprint the caller observed when
     *                                  it verified proof-of-possession
     * @param issuedCert                the freshly signed agent certificate
     * @param enrollmentTokenId         the id of the enrollment token that authorized this
     *                                  re-enrollment
     * @return the updated DTO (post-update state)
     * @throws IllegalStateException if no record with the given {@code _id} and active certificate
     *                               fingerprint is found — the target was concurrently deleted,
     *                               replaced, or modified
     */
    public CollectorInstanceDTO reEnroll(String id,
                                         String expectedActiveFingerprint,
                                         IssuedCertificate issuedCert,
                                         String enrollmentTokenId) {

        final var updates = combine(
                set(FIELD_LAST_SEEN, Date.from(clock.instant())),
                unset(FIELD_NEXT_CERTIFICATE_FINGERPRINT),
                unset(FIELD_NEXT_CERTIFICATE_PEM),
                unset(FIELD_NEXT_CERTIFICATE_EXPIRES_AT),
                set(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, issuedCert.fingerprint()),
                set(FIELD_ACTIVE_CERTIFICATE_PEM, issuedCert.certPem()),
                set(FIELD_ACTIVE_CERTIFICATE_EXPIRES_AT, Date.from(issuedCert.notAfter())),
                set(FIELD_ISSUING_CA_ID, issuedCert.issuerId()),
                set(FIELD_ENROLLMENT_TOKEN_ID, enrollmentTokenId)
        );

        final var updated = collection.findOneAndUpdate(
                Filters.and(MongoUtils.idEq(id), Filters.eq(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, expectedActiveFingerprint)),
                updates,
                new FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER)
        );

        if (updated == null) {
            throw new IllegalStateException(f("Cannot re-enroll. Collector instance with id %s doesn't exist or its active certificate changed concurrently.", id));
        }

        return updated;
    }

    /**
     * Finds the collector instance that has the given value as active or next fingerprint.
     *
     * @param fingerprint the fingerprint to look for
     * @return the found instance or an empty optional
     */
    public Optional<CollectorInstanceDTO> findByActiveOrNextFingerprint(String fingerprint) {
        return Optional.ofNullable(
                collection.find(Filters.or(
                        Filters.eq(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, fingerprint),
                        Filters.eq(FIELD_NEXT_CERTIFICATE_FINGERPRINT, fingerprint)
                )).first()
        );
    }

    /**
     * Sets the next certificate as active for the given instance.
     *
     * @param instance the instance DTO
     * @return true if the activation succeeded, false otherwise
     * @throws IllegalArgumentException if no next certificate exists for the instance
     */
    public boolean activateNextCertificate(CollectorInstanceDTO instance) {
        final Supplier<IllegalArgumentException> err = () -> new IllegalArgumentException("Instance missing next certificate data");

        final var result = collection.updateOne(Filters.eq(FIELD_INSTANCE_UID, instance.instanceUid()), combine(
                set(FIELD_ACTIVE_CERTIFICATE_PEM, instance.nextCertificatePem().orElseThrow(err)),
                set(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, instance.nextCertificateFingerprint().orElseThrow(err)),
                set(FIELD_ACTIVE_CERTIFICATE_EXPIRES_AT, Date.from(instance.nextCertificateExpiresAt().orElseThrow(err))),
                unset(FIELD_NEXT_CERTIFICATE_PEM),
                unset(FIELD_NEXT_CERTIFICATE_FINGERPRINT),
                unset(FIELD_NEXT_CERTIFICATE_EXPIRES_AT)
        ));

        return result.getModifiedCount() > 0;
    }

    /**
     * Inserts the next certificate data for the given instance UID.
     *
     * @param instanceUid the instance UID
     * @param fingerprint the next certificate fingerprint
     * @param pem         the next certificate PEM
     * @param expiresAt   the next certificate expiration date
     * @return true if the insert succeeded, false otherwise
     */
    public boolean insertNextCertificate(String instanceUid, String fingerprint, String pem, Instant expiresAt) {
        final var result = collection.updateOne(Filters.eq(FIELD_INSTANCE_UID, instanceUid), combine(
                set(FIELD_NEXT_CERTIFICATE_PEM, pem),
                set(FIELD_NEXT_CERTIFICATE_FINGERPRINT, fingerprint),
                set(FIELD_NEXT_CERTIFICATE_EXPIRES_AT, Date.from(expiresAt))
        ));
        return result.getModifiedCount() > 0;
    }

    public Optional<CollectorInstanceDTO> findByInstanceUid(String instanceUid) {
        return Optional.ofNullable(
                collection.find(Filters.eq(FIELD_INSTANCE_UID, instanceUid)).first()
        );
    }

    public boolean deleteByInstanceUid(String instanceUid) {
        return collection.deleteOne(Filters.eq(FIELD_INSTANCE_UID, instanceUid)).getDeletedCount() > 0;
    }

    public long deleteExpired(Duration expirationThreshold) {
        final Date cutoff = Date.from(Instant.now(clock).minus(expirationThreshold));
        return collection.deleteMany(Filters.lt(FIELD_LAST_SEEN, cutoff)).getDeletedCount();
    }

    public PaginatedList<CollectorInstanceDTO> findPaginated(Bson query, DbSortResolver.ResolvedSort resolvedSort,
                                                             int page, int perPage, Predicate<CollectorInstanceDTO> selector) {
        return paginationHelper
                .filter(query)
                .sort(resolvedSort.sort())
                .pipeline(resolvedSort.preSortStages())
                .postSortPipeline(resolvedSort.postSortStages())
                .perPage(perPage)
                .page(page, selector);
    }

    public Map<String, InstanceCount> countByFleetGrouped(Instant onlineThreshold) {
        final var pipeline = List.of(
                Aggregates.group("$" + FIELD_FLEET_ID,
                        Accumulators.sum("total", 1L),
                        Accumulators.sum("online",
                                new Document("$cond", List.of(
                                        new Document("$gte", List.of("$" + FIELD_LAST_SEEN, Date.from(onlineThreshold))),
                                        1L,
                                        0L
                                ))
                        )
                )
        );
        final Map<String, InstanceCount> result = new HashMap<>();
        collection.aggregate(pipeline, Document.class).forEach(doc -> {
            final String fleetId = doc.getString("_id");
            if (fleetId != null) {
                result.put(fleetId, new InstanceCount(
                        ((Number) doc.get("total")).longValue(),
                        ((Number) doc.get("online")).longValue()));
            }
        });
        return result;
    }

    public InstanceCount countAcrossAllFleets(Instant onlineThreshold) {
        return countByFleetGrouped(onlineThreshold).values().stream()
                .reduce(new InstanceCount(0L, 0L), (a, b) ->
                        new InstanceCount(a.total() + b.total(), a.online() + b.online()));
    }

    public InstanceCount countByFleet(String fleetId, Instant onlineThreshold) {
        return countByFleetGrouped(onlineThreshold).getOrDefault(fleetId, new InstanceCount(0L, 0L));
    }

    public Map<String, CollectorInstanceDTO> findByInstanceUids(Set<String> instanceUids) {
        return findByInstanceUids(instanceUids, Predicates.alwaysTrue());
    }

    public Map<String, CollectorInstanceDTO> findByInstanceUids(Set<String> instanceUids, Predicate<CollectorInstanceDTO> predicate) {
        if (instanceUids == null || instanceUids.isEmpty()) {
            return Map.of();
        }
        return StreamSupport.stream(collection.find(Filters.in(FIELD_INSTANCE_UID, instanceUids)).spliterator(), false)
                .filter(predicate)
                .collect(Collectors.toMap(CollectorInstanceDTO::instanceUid, Function.identity()));
    }

    /**
     * Extracts the {@link CollectorOSType} from the given report.
     *
     * @param report the report
     * @return the operating system type or UNKNOWN when the attribute doesn't exist
     */
    public static CollectorOSType extractOsTypeFromReport(CollectorInstanceReport report) {
        return extractOSType(report.nonIdentifyingAttributes().orElse(List.of()).stream());
    }

    /**
     * Builds a MongoDB filter selecting the instances that have pending changes, given a
     * {@link PendingChangesLookup}: an instance matches when its {@code last_processed_txn_seq} is
     * behind the newest marker sequence for its fleet or for the instance itself. Negate the result
     * (e.g. {@link Filters#nor}) to select instances that are in sync. When the lookup is empty (no
     * markers anywhere), nothing is pending, so the returned filter matches no document.
     */
    public static Bson hasPendingChangesFilter(PendingChangesLookup pendingChangesLookup) {
        final List<Bson> clauses = new ArrayList<>();

        pendingChangesLookup.maxByInstanceUid().forEach((uid, maxSeq) ->
                clauses.add(Filters.and(
                        Filters.eq(FIELD_INSTANCE_UID, uid),
                        Filters.lt(FIELD_LAST_PROCESSED_TXN_SEQ, maxSeq))
                ));
        pendingChangesLookup.maxByFleetId().forEach((fleetId, maxSeq) ->
                clauses.add(Filters.and(
                        Filters.eq(FIELD_FLEET_ID, fleetId),
                        Filters.lt(FIELD_LAST_PROCESSED_TXN_SEQ, maxSeq))
                ));

        return clauses.isEmpty() ? Filters.in(FIELD_ID, List.of()) : Filters.or(clauses);
    }

    private static CollectorOSType extractOSType(Stream<Attribute> attributes) {
        return attributes.filter(a -> OS_TYPE_KEY.equals(a.key()))
                .map(Attribute::value)
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .map(CollectorOSType::of)
                .findFirst()
                .orElse(CollectorOSType.UNKNOWN);
    }

    public record MinimalCollectorInstanceDTO(@Id @JsonProperty(FIELD_ID) String id,
                                              @JsonProperty(FIELD_FLEET_ID) String fleetId,
                                              @JsonProperty(FIELD_MESSAGE_SEQ_NUM) long messageSeqNum,
                                              @JsonProperty(FIELD_LAST_PROCESSED_TXN_SEQ) long lastProcessTxnSeq,
                                              @JsonProperty(FIELD_NON_IDENTIFYING_ATTRIBUTES) List<Attribute> nonIdentifyingAttributes) {
        public CollectorOSType osType() {
            if (nonIdentifyingAttributes == null) {
                return CollectorOSType.UNKNOWN;
            }
            return extractOSType(nonIdentifyingAttributes.stream());
        }
    }

    public record InstanceCount(long total, long online) {
        public long offline() {
            return total() - online();
        }
    }
}
