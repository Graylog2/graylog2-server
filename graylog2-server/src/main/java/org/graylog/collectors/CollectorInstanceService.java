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
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.collectors.events.CollectorInstanceCertsChangedEvent;
import org.graylog.collectors.opamp.IssuedCertificate;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
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
import java.util.Objects;
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
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_CERTIFICATES_ROTATED_AT;
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
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_EXPIRES_AT;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_PREVIOUS_CERTIFICATE_PEM;
import static org.graylog2.database.MongoEntity.FIELD_ID;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class CollectorInstanceService {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorInstanceService.class);
    private static final String OS_TYPE_KEY = "os.type";
    public static final String COLLECTION_NAME = "collector_instances";

    private static final int REVOCATION_EVENT_BATCH_SIZE = 1000; // not more than this many fingerprints per event
    private static final long MAX_DELETIONS_PER_RUN = 100_000; // safety net for deleteExpired
    // How long a superseded (previous-slot) certificate remains valid on the ingest path after rotation,
    // covering the collector's asynchronous switch to the new certificate. TODO: make configurable.
    private static final Duration PREVIOUS_CERT_GRACE_TTL = Duration.ofMinutes(5);

    private final MongoCollection<CollectorInstanceDTO> collection;
    private final MongoPaginationHelper<CollectorInstanceDTO> paginationHelper;
    private final com.mongodb.client.MongoCollection<MinimalCollectorInstanceDTO> projectedCollection;

    private final MongoCollections mongoCollections;
    private final ClusterEventBus clusterEventBus;
    private final Clock clock;

    @Inject
    public CollectorInstanceService(MongoCollections mongoCollections, ClusterEventBus clusterEventBus, Clock clock) {
        collection = mongoCollections.collection(COLLECTION_NAME, CollectorInstanceDTO.class);
        projectedCollection = mongoCollections.nonEntityCollection(COLLECTION_NAME, MinimalCollectorInstanceDTO.class);
        paginationHelper = mongoCollections.paginationHelper(collection);
        this.mongoCollections = mongoCollections;
        this.clusterEventBus = clusterEventBus;
        this.clock = clock;

        // Clean up legacy null values which would prevent creating a unique index below
        try {
            final var updated = collection.updateMany(Filters.type(FIELD_NEXT_CERTIFICATE_FINGERPRINT, BsonType.NULL),
                    Updates.combine(Updates.unset(FIELD_NEXT_CERTIFICATE_FINGERPRINT),
                            Updates.unset(FIELD_NEXT_CERTIFICATE_PEM),
                            Updates.unset(FIELD_NEXT_CERTIFICATE_EXPIRES_AT)
                    )
            );
            if (updated.getModifiedCount() > 0) {
                LOG.info("Cleaned up {} legacy null values for the \"next_certificate_*\" fields", updated.getModifiedCount());
            }
        } catch (Exception e) {
            LOG.error("Cleaning up legacy null values for the \"next_certificate_*\" fields failed", e);
        }

        try {
            collection.createIndexes(List.of(
                    new IndexModel(Indexes.ascending(FIELD_INSTANCE_UID), new IndexOptions().unique(true)),
                    new IndexModel(Indexes.ascending(FIELD_IDENTIFYING_ATTRIBUTES + ".key",
                            FIELD_IDENTIFYING_ATTRIBUTES + ".value")),
                    new IndexModel(Indexes.ascending(FIELD_NON_IDENTIFYING_ATTRIBUTES + ".key",
                            FIELD_NON_IDENTIFYING_ATTRIBUTES + ".value")),
                    new IndexModel(Indexes.ascending(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT),
                            new IndexOptions().unique(true)),
                    new IndexModel(Indexes.ascending(FIELD_NEXT_CERTIFICATE_FINGERPRINT),
                            new IndexOptions().partialFilterExpression(Filters.exists(FIELD_NEXT_CERTIFICATE_FINGERPRINT))
                                    .unique(true)),
                    new IndexModel(Indexes.ascending(FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT),
                            new IndexOptions().partialFilterExpression(Filters.exists(FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT))
                                    .unique(true)),
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
     * <p>
     * Publishes a {@link CollectorInstanceCertsChangedEvent} for the new active fingerprint.
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

        clusterEventBus.post(new CollectorInstanceCertsChangedEvent(Set.of(issuedCert.fingerprint())));

        return dto.toBuilder()
                .id(insertedIdAsString(insertOneResult))
                .build();
    }

    /**
     * Atomically re-issues a collector's active certificate on an existing record.
     * <p>
     * The update is a compare-and-swap: it only matches the record whose Mongo {@code _id}, active
     * certificate fingerprint, and next certificate fingerprint equal those of the given
     * {@code instance}. Callers must pass the instance they observed when they verified the
     * re-enrollment request (typically via {@link #findByInstanceUid}). This ensures the update only
     * lands on the exact state the caller validated. If the record has since been deleted, replaced,
     * or had its certificates changed (e.g. by a concurrent renewal activation), the update matches
     * nothing and this method throws.
     * <p>
     * The caller (typically {@code OpAmpService.handleEnrollment}) is responsible for enforcing
     * proof-of-possession (matching the CSR public key against the stored active certificate's
     * public key) before calling this method. This service method performs no such check — it
     * only enforces the compare-and-swap race guard.
     * <p>
     * Publishes a {@link CollectorInstanceCertsChangedEvent} for every fingerprint this touches — the new
     * active fingerprint plus the replaced active, next, and previous fingerprints — so subscribers
     * re-resolve them all (the replaced ones resolve to nothing, since they were cleared).
     *
     * @param existingInstance  the record state the caller validated (supplies the {@code _id} and the
     *                          active and next fingerprints matched by the compare-and-swap)
     * @param issuedCert        the freshly signed agent certificate
     * @param enrollmentTokenId the id of the enrollment token that authorized this re-enrollment
     * @return the updated DTO (post-update state)
     * @throws IllegalStateException if no record matching the instance's {@code _id} and certificate
     *                               fingerprints is found — the target was concurrently deleted,
     *                               replaced, or modified
     */
    public CollectorInstanceDTO reEnroll(CollectorInstanceDTO existingInstance,
                                         IssuedCertificate issuedCert,
                                         String enrollmentTokenId) {

        final var updates = combine(
                set(FIELD_LAST_SEEN, Date.from(clock.instant())),
                unset(FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT),
                unset(FIELD_PREVIOUS_CERTIFICATE_PEM),
                unset(FIELD_PREVIOUS_CERTIFICATE_EXPIRES_AT),
                unset(FIELD_NEXT_CERTIFICATE_FINGERPRINT),
                unset(FIELD_NEXT_CERTIFICATE_PEM),
                unset(FIELD_NEXT_CERTIFICATE_EXPIRES_AT),
                unset(FIELD_CERTIFICATES_ROTATED_AT),
                set(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, issuedCert.fingerprint()),
                set(FIELD_ACTIVE_CERTIFICATE_PEM, issuedCert.certPem()),
                set(FIELD_ACTIVE_CERTIFICATE_EXPIRES_AT, Date.from(issuedCert.notAfter())),
                set(FIELD_ISSUING_CA_ID, issuedCert.issuerId()),
                set(FIELD_ENROLLMENT_TOKEN_ID, enrollmentTokenId)
        );

        final var updated = collection.findOneAndUpdate(
                Filters.and(
                        MongoUtils.idEq(
                                Objects.requireNonNull(existingInstance.id(), "Instance ID must not be null for re-enrollment")
                        ),
                        Filters.eq(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, existingInstance.activeCertificateFingerprint()),
                        Filters.eq(FIELD_NEXT_CERTIFICATE_FINGERPRINT, existingInstance.nextCertificateFingerprint().orElse(null)),
                        Filters.eq(FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT, existingInstance.previousCertificateFingerprint().orElse(null))
                ),
                updates,
                new FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER)
        );

        if (updated == null) {
            throw new IllegalStateException(f("Cannot re-enroll. Collector instance with id %s doesn't exist or its active certificate changed concurrently.", existingInstance.id()));
        }


        clusterEventBus.post(new CollectorInstanceCertsChangedEvent(
                Sets.union(existingInstance.allCertFingerprints(), updated.allCertFingerprints()))
        );

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
     * Resolves a client-certificate fingerprint to the collector instance it binds to. This is a pure
     * mapping of the instance's certificate slots — no clock is consulted: every binding carries the
     * instant it stops being honored on the ingest path ({@link CertBinding#validUntil()}: the
     * certificate's own expiry, capped for a superseded previous-slot certificate by the renewal grace
     * deadline {@code certificates_rotated_at + PREVIOUS_CERT_GRACE_TTL}). Callers evaluate validity via
     * {@link CertBinding#isValidAt(Instant)}. A returned binding may already be expired.
     *
     * @param fingerprint the presented certificate fingerprint
     * @return the binding, or an empty optional if no instance has this fingerprint in any slot
     */
    public Optional<CertBinding> resolveCertBinding(String fingerprint) {
        return findByActiveOrNextOrPreviousFingerprint(fingerprint)
                .map(instance -> {
                    if (fingerprint.equals(instance.activeCertificateFingerprint())) {
                        return new CertBinding(instance.instanceUid(), instance.activeCertificateExpiresAt());
                    }
                    if (instance.previousCertificateFingerprint().filter(fingerprint::equals).isPresent()) {
                        final var graceDeadline = instance.certificatesRotatedAt()
                                .map(rotatedAt -> rotatedAt.plus(PREVIOUS_CERT_GRACE_TTL))
                                .orElse(Instant.MIN);
                        final var certExpiry = instance.previousCertificateExpiresAt().orElse(Instant.MIN);
                        return new CertBinding(instance.instanceUid(),
                                graceDeadline.isBefore(certExpiry) ? graceDeadline : certExpiry);
                    }
                    if (instance.nextCertificateFingerprint().filter(fingerprint::equals).isPresent()) {
                        final var certExpiry = instance.nextCertificateExpiresAt().orElse(Instant.MIN);
                        return new CertBinding(instance.instanceUid(), certExpiry);
                    }
                    return null;
                });
    }

    /**
     * Finds the collector instance that has the given value as its active, next, or previous certificate
     * fingerprint. Internal building block for {@link #resolveCertBinding(String)}; it yields the raw
     * document without the binding's validity stamp, so it is not exposed directly.
     */
    private Optional<CollectorInstanceDTO> findByActiveOrNextOrPreviousFingerprint(String fingerprint) {
        return Optional.ofNullable(
                collection.find(Filters.or(
                        Filters.eq(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, fingerprint),
                        Filters.eq(FIELD_NEXT_CERTIFICATE_FINGERPRINT, fingerprint),
                        Filters.eq(FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT, fingerprint)
                )).first()
        );
    }

    /**
     * Promotes the instance's next certificate to active. The superseded active certificate is demoted to
     * the previous slot (kept resolvable on the ingest path for the grace window measured from
     * {@code certificates_rotated_at}, which this stamps to now) and the next slot is cleared.
     *
     * Publishes a {@link CollectorInstanceCertsChangedEvent} for the rotated fingerprints (promoted,
     * demoted, and any displaced previous) so subscribers re-resolve them.
     *
     * @param instance the instance DTO
     * @return true if the activation succeeded, false otherwise
     * @throws IllegalArgumentException if no next certificate exists for the instance
     */
    public boolean activateNextCertificate(CollectorInstanceDTO instance) {
        final Supplier<IllegalArgumentException> err = () -> new IllegalArgumentException("Instance missing next certificate data");

        final var orig = collection.findOneAndUpdate(Filters.eq(FIELD_INSTANCE_UID, instance.instanceUid()), combine(
                set(FIELD_PREVIOUS_CERTIFICATE_PEM, instance.activeCertificatePem()),
                set(FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT, instance.activeCertificateFingerprint()),
                set(FIELD_PREVIOUS_CERTIFICATE_EXPIRES_AT, Date.from(instance.activeCertificateExpiresAt())),
                set(FIELD_ACTIVE_CERTIFICATE_PEM, instance.nextCertificatePem().orElseThrow(err)),
                set(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT, instance.nextCertificateFingerprint().orElseThrow(err)),
                set(FIELD_ACTIVE_CERTIFICATE_EXPIRES_AT, Date.from(instance.nextCertificateExpiresAt().orElseThrow(err))),
                set(FIELD_CERTIFICATES_ROTATED_AT, Date.from(clock.instant())),
                unset(FIELD_NEXT_CERTIFICATE_PEM),
                unset(FIELD_NEXT_CERTIFICATE_FINGERPRINT),
                unset(FIELD_NEXT_CERTIFICATE_EXPIRES_AT)
        ), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));

        if (orig != null) {
            clusterEventBus.post(new CollectorInstanceCertsChangedEvent(
                    Sets.union(instance.allCertFingerprints(), orig.allCertFingerprints()))
            );
        }

        return orig != null;
    }

    /**
     * Inserts the next certificate data for the given instance UID.
     * <p>
     * Publishes a {@link CollectorInstanceCertsChangedEvent} for the new next fingerprint and the
     * fingerprints already on the instance, so subscribers re-resolve them.
     *
     * @param instanceUid the instance UID
     * @param fingerprint the next certificate fingerprint
     * @param pem         the next certificate PEM
     * @param expiresAt   the next certificate expiration date
     * @return true if the insert succeeded, false otherwise
     */
    public boolean insertNextCertificate(String instanceUid, String fingerprint, String pem, Instant expiresAt) {
        final var orig = collection.findOneAndUpdate(Filters.eq(FIELD_INSTANCE_UID, instanceUid), combine(
                set(FIELD_NEXT_CERTIFICATE_PEM, pem),
                set(FIELD_NEXT_CERTIFICATE_FINGERPRINT, fingerprint),
                set(FIELD_NEXT_CERTIFICATE_EXPIRES_AT, Date.from(expiresAt))
        ), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));

        if (orig != null) {
            clusterEventBus.post(new CollectorInstanceCertsChangedEvent(
                    Sets.union(orig.allCertFingerprints(), Set.of(fingerprint)))
            );
        }

        return orig != null;
    }

    /**
     * Finds the collector instance with the given instance UID.
     *
     * @return the instance, or an empty optional if none exists
     */
    public Optional<CollectorInstanceDTO> findByInstanceUid(String instanceUid) {
        return Optional.ofNullable(
                collection.find(Filters.eq(FIELD_INSTANCE_UID, instanceUid)).first()
        );
    }

    /**
     * Deletes the collector instance with the given instance UID and, if one was deleted, publishes a
     * {@link CollectorInstanceCertsChangedEvent} for all of its certificate fingerprints so subscribers
     * re-resolve them (and drop them, since the instance is gone).
     *
     * @return true if an instance was deleted, false if none matched
     */
    public boolean deleteByInstanceUid(String instanceUid) {
        final var deleted = collection.findOneAndDelete(Filters.eq(FIELD_INSTANCE_UID, instanceUid));
        if (deleted != null) {
            clusterEventBus.post(new CollectorInstanceCertsChangedEvent(deleted.allCertFingerprints()));
        }

        return deleted != null;
    }

    /**
     * Deletes collector instances whose {@code last_seen} is older than {@code expirationThreshold},
     * one at a time via {@link com.mongodb.client.MongoCollection#findOneAndDelete}, and publishes the
     * deleted certificate fingerprints in {@link CollectorInstanceCertsChangedEvent}s,
     * batched by {@link #REVOCATION_EVENT_BATCH_SIZE}. Deletes at most {@link #MAX_DELETIONS_PER_RUN}
     * instances per call.
     *
     * @return the number of instances deleted
     */
    public long deleteExpired(Duration expirationThreshold) {
        final Date cutoff = Date.from(Instant.now(clock).minus(expirationThreshold));

        final var coll = mongoCollections.nonEntityCollection(COLLECTION_NAME, DeletedInstance.class);

        final var deleted = new ArrayList<DeletedInstance>();

        while (deleted.size() < MAX_DELETIONS_PER_RUN) { // safety net
            final var deletedInstance = coll.findOneAndDelete(Filters.lt(FIELD_LAST_SEEN, cutoff),
                    new FindOneAndDeleteOptions().projection(
                            Projections.include(
                                    FIELD_ID,
                                    FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT,
                                    FIELD_ACTIVE_CERTIFICATE_FINGERPRINT,
                                    FIELD_NEXT_CERTIFICATE_FINGERPRINT))
            );

            if (deletedInstance == null) {
                break; // We are done. There are no more expired instances.
            }

            deleted.add(deletedInstance);
        }

        if (deleted.size() == MAX_DELETIONS_PER_RUN) {
            LOG.warn("Exceptionally high number of expired Collectors. Expiration cleanup might not be able to keep up.");
        }

        final var revokedCertFingerprints = deleted.stream()
                .flatMap(DeletedInstance::allCertFingerprints)
                .collect(Collectors.toSet());

        Iterables.partition(revokedCertFingerprints, REVOCATION_EVENT_BATCH_SIZE).forEach(batch -> {
            clusterEventBus.post(new CollectorInstanceCertsChangedEvent(revokedCertFingerprints));
        });

        return deleted.size();
    }

    /**
     * Streams collector instances last seen at or after {@code now - expirationThreshold} (i.e. not yet
     * expired), most-recently-seen first. The caller must close the returned stream.
     */
    @MustBeClosed
    public Stream<CollectorInstanceDTO> streamAllUnexpired(Duration expirationThreshold) {
        final Date cutoff = Date.from(Instant.now(clock).minus(expirationThreshold));
        return MongoUtils.stream(
                collection.find(Filters.gte(FIELD_LAST_SEEN, cutoff))
                        .sort(Sorts.descending(FIELD_LAST_SEEN)));
    }

    private record DeletedInstance(@Id @JsonProperty(FIELD_ID) String id,
                                   @JsonProperty(FIELD_PREVIOUS_CERTIFICATE_FINGERPRINT) String previousFingerprint,
                                   @JsonProperty(FIELD_ACTIVE_CERTIFICATE_FINGERPRINT) String activeFingerprint,
                                   @JsonProperty(FIELD_NEXT_CERTIFICATE_FINGERPRINT) String nextFingerprint) {
        public Stream<String> allCertFingerprints() {
            return Stream.of(
                    Optional.ofNullable(previousFingerprint()),
                    Optional.of(activeFingerprint()),
                    Optional.ofNullable(nextFingerprint())
            ).flatMap(Optional::stream);
        }
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
