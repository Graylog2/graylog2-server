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
package org.graylog.scheduler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import one.util.streamex.StreamEx;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.scheduler.capabilities.SchedulerCapabilitiesService;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.utilities.MongoQueryUtils;
import org.joda.time.DateTime;
import org.mongojack.DBQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static java.util.Objects.requireNonNull;
import static org.graylog.scheduler.JobSchedulerConfiguration.LOCK_EXPIRATION_DURATION;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.stream;

// This class does NOT use PaginatedDbService because we use the triggers collection for locking and need to handle
// updates very carefully.
public class DBJobTriggerService {
    public static final String COLLECTION_NAME = "scheduler_triggers";
    private static final String FIELD_ID = "_id";
    static final String FIELD_JOB_DEFINITION_ID = JobTriggerDto.FIELD_JOB_DEFINITION_ID;
    private static final String FIELD_LOCK_OWNER = JobTriggerDto.FIELD_LOCK + "." + JobTriggerLock.FIELD_OWNER;
    private static final String FIELD_LAST_LOCK_OWNER = JobTriggerDto.FIELD_LOCK + "." + JobTriggerLock.FIELD_LAST_OWNER;
    private static final String FIELD_PROGRESS = JobTriggerDto.FIELD_LOCK + "." + JobTriggerLock.FIELD_PROGRESS;
    private static final String FIELD_LAST_LOCK_TIME = JobTriggerDto.FIELD_LOCK + "." + JobTriggerLock.FIELD_LAST_LOCK_TIME;
    private static final String FIELD_NEXT_TIME = JobTriggerDto.FIELD_NEXT_TIME;
    private static final String FIELD_START_TIME = JobTriggerDto.FIELD_START_TIME;
    private static final String FIELD_END_TIME = JobTriggerDto.FIELD_END_TIME;
    private static final String FIELD_STATUS = JobTriggerDto.FIELD_STATUS;
    private static final String FIELD_SCHEDULE = JobTriggerDto.FIELD_SCHEDULE;
    private static final String FIELD_DATA = JobTriggerDto.FIELD_DATA;
    private static final String FIELD_UPDATED_AT = JobTriggerDto.FIELD_UPDATED_AT;
    private static final String FIELD_CONCURRENCY_RESCHEDULE_COUNT = JobTriggerDto.FIELD_CONCURRENCY_RESCHEDULE_COUNT;
    private static final String FIELD_TRIGGERED_AT = JobTriggerDto.FIELD_TRIGGERED_AT;
    private static final String FIELD_CONSTRAINTS = JobTriggerDto.FIELD_CONSTRAINTS;
    private static final String FIELD_LAST_EXECUTION_DURATION = JobTriggerDto.FIELD_EXECUTION_DURATION;
    private static final String FIELD_IS_CANCELLED = JobTriggerDto.FIELD_IS_CANCELLED;
    private static final String FIELD_JOB_DEFINITION_TYPE = JobTriggerDto.FIELD_JOB_DEFINITION_TYPE;

    private final String nodeId;
    private final JobSchedulerClock clock;
    private final SchedulerCapabilitiesService schedulerCapabilitiesService;
    private final Duration lockExpirationDuration;
    private final MongoCollection<JobTriggerDto> collection;
    private MongoUtils<JobTriggerDto> mongoUtils;

    @Inject
    public DBJobTriggerService(MongoCollections mongoCollections,
                               NodeId nodeId,
                               JobSchedulerClock clock,
                               SchedulerCapabilitiesService schedulerCapabilitiesService,
                               @Named(LOCK_EXPIRATION_DURATION) Duration lockExpirationDuration) {
        this.nodeId = nodeId.getNodeId();
        this.clock = clock;
        this.schedulerCapabilitiesService = schedulerCapabilitiesService;
        this.lockExpirationDuration = lockExpirationDuration;
        this.collection = mongoCollections.get(COLLECTION_NAME, JobTriggerDto.class);
        this.mongoUtils = mongoCollections.utils(collection);

        collection.createIndex(new BasicDBObject(FIELD_JOB_DEFINITION_ID, 1));
        collection.createIndex(new BasicDBObject(FIELD_LOCK_OWNER, 1));
        collection.createIndex(new BasicDBObject(FIELD_STATUS, 1));
        collection.createIndex(new BasicDBObject(FIELD_START_TIME, 1));
        collection.createIndex(new BasicDBObject(FIELD_END_TIME, 1));
        collection.createIndex(new BasicDBObject(FIELD_NEXT_TIME, 1));
        collection.createIndex(new BasicDBObject(FIELD_CONSTRAINTS, 1));
        collection.createIndex(new BasicDBObject(FIELD_JOB_DEFINITION_TYPE, 1));
    }

    @SuppressWarnings("unused")
    @Deprecated
    public DBJobTriggerService(MongoConnection mongoConnection,
                               MongoCollections mongoCollections,
                               MongoJackObjectMapperProvider mapper,
                               NodeId nodeId,
                               JobSchedulerClock clock,
                               SchedulerCapabilitiesService schedulerCapabilitiesService,
                               @Named(LOCK_EXPIRATION_DURATION) Duration lockExpirationDuration) {
        this(mongoCollections, nodeId, clock, schedulerCapabilitiesService, lockExpirationDuration);
    }

    /**
     * Loads all existing records and returns them.
     *
     * @return list of records
     */
    public List<JobTriggerDto> all() {
        return stream(collection.find().sort(descending(FIELD_ID))).toList();
    }

    /**
     * Loads the record for the given ID.
     *
     * @param id record ID to load
     * @return filled optional when the record exists, an empty optional otherwise
     */
    public Optional<JobTriggerDto> get(String id) {
        return mongoUtils.getById(id);
    }

    /**
     * Returns one trigger for the given job definition ID.
     * TODO: Don't throw exception when there is more than one trigger for a job definition. (see source code)
     *
     * @param jobDefinitionId the job definition ID
     * @return One found job trigger
     */
    public Optional<JobTriggerDto> getOneForJob(String jobDefinitionId) {
        final List<JobTriggerDto> triggers = getAllForJob(jobDefinitionId);
        // We are currently expecting only one trigger per job definition. This will most probably change in the
        // future once we extend our scheduler usage.
        // TODO: Don't throw exception when there is more than one trigger for a job definition.
        //       To be able to do this, we need some kind of label system to make sure we can differentiate between
        //       automatically created triggers (e.g. by event definition) and manually created ones.
        if (triggers.size() > 1) {
            throw new IllegalStateException("More than one trigger for job definition <" + jobDefinitionId + ">");
        }
        return triggers.stream().findFirst();
    }

    public List<JobTriggerDto> getAllForJob(String jobDefinitionId) {
        if (isNullOrEmpty(jobDefinitionId)) {
            throw new IllegalArgumentException("jobDefinitionId cannot be null or empty");
        }

        return stream(collection.find(eq(FIELD_JOB_DEFINITION_ID, jobDefinitionId))).toList();
    }

    /**
     * Returns all job triggers for the given job definition IDs, grouped by job definition ID.
     * TODO: Don't throw exception when there is more than one trigger for a job definition. (see source code)
     *
     * @param jobDefinitionIds the job definition IDs
     * @return map of found job triggers
     */
    public Map<String, List<JobTriggerDto>> getForJobs(Collection<String> jobDefinitionIds) {
        if (jobDefinitionIds == null) {
            throw new IllegalArgumentException("jobDefinitionIds cannot be null");
        }

        final Set<String> queryValues = jobDefinitionIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !isNullOrEmpty(id))
                .collect(Collectors.toSet());

        final var filter = in(FIELD_JOB_DEFINITION_ID, queryValues);
        final Map<String, List<JobTriggerDto>> groupedTriggers = StreamEx.of(stream(collection.find(filter)))
                .groupingBy(JobTriggerDto::jobDefinitionId);

        // We are currently expecting only one trigger per job definition. This will most probably change in the
        // future once we extend our scheduler usage.
        // TODO: Don't throw exception when there is more than one trigger for a job definition.
        //       To be able to do this, we need some kind of label system to make sure we can differentiate between
        //       automatically created triggers (e.g. by event definition) and manually created ones.
        for (Map.Entry<String, List<JobTriggerDto>> entry : groupedTriggers.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new IllegalStateException("More than one trigger for job definition <" + entry.getKey() + ">");
            }
        }

        return groupedTriggers;
    }

    /**
     * Creates a new record in the database. The given {@link JobTriggerDto} object <b>must not</b> have an ID to make
     * sure a new record is created.
     *
     * @param trigger the new trigger object (without an ID set)
     * @return the newly created trigger object
     * @throws IllegalArgumentException if the passed trigger has an ID set
     */
    public JobTriggerDto create(JobTriggerDto trigger) {
        requireNonNull(trigger, "trigger cannot be null");

        // Make sure we don't save triggers that have an ID. That would potentially overwrite an existing trigger
        // and destroy locks and other data.
        if (trigger.id() != null) {
            throw new IllegalArgumentException("New trigger must not have an ID");
        }

        var id = insertedIdAsString(collection.insertOne(trigger));
        return trigger.toBuilder().id(id).build();
    }

    /**
     * Updates the given trigger record in the database. This method takes care of not overwriting any locks and
     * state data with the update.
     *
     * @param trigger the trigger to update
     * @return true when the update was successful, false otherwise
     * @throws IllegalArgumentException if the passed trigger doesn't have an ID set
     */
    public boolean update(JobTriggerDto trigger) {
        requireNonNull(trigger, "trigger cannot be null");

        // Make sure we don't update triggers that don't have an ID. This would create a new record instead of updating
        // an existing one.
        if (isNullOrEmpty(trigger.id())) {
            throw new IllegalArgumentException("Trigger must have an ID");
        }

        // We don't want to allow updating all fields of the trigger. That's why we can't just use "save(JobTriggerDto)"
        // because that would overwrite fields like "lock" and others we don't want to update.
        final List<Bson> updates = new ArrayList<>(List.of(
                set(FIELD_START_TIME, trigger.startTime()),
                set(FIELD_NEXT_TIME, trigger.nextTime()),
                set(FIELD_DATA, trigger.data()),
                set(FIELD_UPDATED_AT, clock.nowUTC()),
                set(FIELD_CONCURRENCY_RESCHEDULE_COUNT, trigger.concurrencyRescheduleCount()))
        );

        if (trigger.endTime().isPresent()) {
            updates.add(set(FIELD_END_TIME, trigger.endTime()));
        }

        // TODO: can we simplify this now?

        // We cannot just use "update.set(FIELD_SCHEDULE, trigger.schedule()" to update the trigger because mongojack
        // has an issue with serializing polymorphic classes and "$set": https://github.com/mongojack/mongojack/issues/101
        // That's why JobSchedule objects have the "toDBUpdate()" method to give us all fields for the specific
        // schedule implementation. (the fields can be different, depending on the schedule type)
        final Optional<Map<String, Object>> scheduleUpdate = trigger.schedule().toDBUpdate(FIELD_SCHEDULE + ".");
        if (scheduleUpdate.isPresent()) {
            // First load the old trigger so we can compare the scheduler config keys.
            final JobTriggerDto oldTrigger = get(trigger.id())
                    .orElseThrow(() -> new IllegalStateException("Couldn't find trigger with ID " + trigger.id()));

            // Compute old and new schedule config keys
            final Set<String> oldKeys = oldTrigger.schedule().toDBUpdate(FIELD_SCHEDULE + ".")
                    .orElse(new HashMap<>()).keySet();
            final Set<String> newKeys = scheduleUpdate.get().keySet();

            // Find out which keys aren't present in the new schedule config.
            final Sets.SetView<String> toUnset = Sets.difference(oldKeys, newKeys);

            // Remove keys which aren't present in the new schedule config. Otherwise we would have old keys in there
            // which cannot be parsed for the updated schedule type.
            toUnset.forEach(key -> updates.add(unset(key)));

            // Then we can set the specific fields.
            scheduleUpdate.get().forEach((k, v) -> updates.add(set(k, v)));
        }

        return collection.updateOne(eq(FIELD_ID, getId(trigger)), combine(updates)).getModifiedCount() > 0;
    }

    /**
     * Deletes the trigger with the given ID.
     *
     * @param triggerId the trigger ID to delete
     * @return true if the trigger got deleted, false otherwise
     */
    public boolean delete(String triggerId) {
        if (isNullOrEmpty(triggerId)) {
            throw new IllegalArgumentException("triggerId cannot be null or empty");
        }
        return mongoUtils.deleteById(triggerId);
    }

    /**
     * Deletes completed / cancelled {@link OnceJobSchedule} triggers that are older than the provided time
     *
     * @param timeValue the time range of triggers to be removed
     * @param unit      the unit of the provided timeValue
     * @return the number of deleted triggers
     */
    public int deleteCompletedOnceSchedulesOlderThan(long timeValue, TimeUnit unit) {
        final var filter = and(
                eq(FIELD_LOCK_OWNER, null),
                or(
                        eq(FIELD_STATUS, JobTriggerStatus.COMPLETE),
                        eq(FIELD_STATUS, JobTriggerStatus.CANCELLED)
                ),
                eq(FIELD_SCHEDULE + "." + JobSchedule.TYPE_FIELD, OnceJobSchedule.TYPE_NAME),
                lt(FIELD_UPDATED_AT, clock.nowUTC().minus(unit.toMillis(timeValue)))
        );
        return (int) collection.deleteMany(filter).getDeletedCount();
    }

    /**
     * Deletes job triggers using the given query. <em>Use judiciously</em>, as will make assumptions about the
     * internal data structure of triggers.
     */
    public int deleteByQuery(Bson query) {
        return (int) collection.deleteMany(query).getDeletedCount();
    }

    @Deprecated
    public int deleteByQuery(DBQuery.Query query) {
        mongoUtils.initializeLegacyMongoJackBsonObject(query);
        return deleteByQuery((Bson) query);
    }

    public long countByQuery(Bson query) {
        return collection.countDocuments(query);
    }

    @Deprecated
    public long countByQuery(DBQuery.Query query) {
        mongoUtils.initializeLegacyMongoJackBsonObject(query);
        return countByQuery((Bson) query);
    }

    /**
     * Locks and returns the next runnable trigger. The caller needs to take care of releasing the trigger lock.
     *
     * @return next runnable trigger if any exists, an empty {@link Optional} otherwise
     */
    public Optional<JobTriggerDto> nextRunnableTrigger() {
        final DateTime now = clock.nowUTC();

        final var constraintsQuery = MongoQueryUtils.getArrayIsContainedQuery(FIELD_CONSTRAINTS, schedulerCapabilitiesService.getNodeCapabilities());

        final var filter = or(and(
                        // We cannot lock a trigger that is already locked by another node
                        eq(FIELD_LOCK_OWNER, null),
                        eq(FIELD_STATUS, JobTriggerStatus.RUNNABLE),
                        lte(FIELD_START_TIME, now),
                        constraintsQuery,

                        or( // Skip triggers that have an endTime which is due
                                not(exists(FIELD_END_TIME)),
                                eq(FIELD_END_TIME, null),
                                gt(FIELD_END_TIME, Optional.of(now))
                        ),
                        // TODO: Using the wall clock time here can be problematic if the node time is off
                        //       The scheduler should not lock any new triggers if it detects that its clock is wrong
                        lte(FIELD_NEXT_TIME, now)
                ), and(
                        ne(FIELD_LOCK_OWNER, null),
                        ne(FIELD_LOCK_OWNER, nodeId),
                        eq(FIELD_STATUS, JobTriggerStatus.RUNNING),
                        constraintsQuery,
                        lt(FIELD_LAST_LOCK_TIME, now.minus(lockExpirationDuration.toMilliseconds())))
        );
        // We want to lock the trigger with the oldest next time
        final var sort = ascending(FIELD_NEXT_TIME);

        final var lockUpdate = combine(
                set(FIELD_LOCK_OWNER, nodeId),
                set(FIELD_LAST_LOCK_OWNER, nodeId),
                set(FIELD_STATUS, JobTriggerStatus.RUNNING),
                set(FIELD_TRIGGERED_AT, Optional.of(now)),
                set(FIELD_LAST_LOCK_TIME, now)
        );

        // Atomically update, lock and return the next runnable trigger
        final JobTriggerDto trigger = collection.findOneAndUpdate(filter, lockUpdate,
                new FindOneAndUpdateOptions().sort(sort).returnDocument(ReturnDocument.AFTER)
        );

        return Optional.ofNullable(trigger);
    }

    /**
     * Releases a locked trigger. The trigger is only released if it's owned by the calling node.
     *
     * @param trigger       trigger that should be released
     * @param triggerUpdate update to apply to the trigger
     * @return true if the trigger has been modified, false otherwise
     */
    public boolean releaseTrigger(JobTriggerDto trigger, JobTriggerUpdate triggerUpdate) {
        requireNonNull(trigger, "trigger cannot be null");
        requireNonNull(triggerUpdate, "triggerUpdate cannot be null");

        final var filter = and(
                // Make sure that the owner still owns the trigger
                eq(FIELD_LOCK_OWNER, nodeId),
                eq(FIELD_ID, getId(trigger)),
                // Only release running triggers. The trigger might have been paused while the trigger was running
                // so we don't want to set it to RUNNABLE again.
                // TODO: This is an issue. If a user set it to PAUSED, we will not unlock it. Figure something out.
                //       Maybe a manual trigger pause will set "nextStatus" if the trigger is currently running?
                //       That next status would need to be set on release.
                eq(FIELD_STATUS, JobTriggerStatus.RUNNING)
        );

        final List<Bson> updates = new ArrayList<>();
        updates.add(unset(FIELD_LOCK_OWNER));

        if (triggerUpdate.concurrencyReschedule()) {
            updates.add(inc(FIELD_CONCURRENCY_RESCHEDULE_COUNT, 1));
        } else {
            updates.add(set(FIELD_CONCURRENCY_RESCHEDULE_COUNT, 0));
        }

        // An empty next time indicates that this trigger should not be fired anymore. (e.g. for "once" schedules)
        if (triggerUpdate.nextTime().isPresent()) {
            if (triggerUpdate.status().isPresent()) {
                updates.add(set(FIELD_STATUS, triggerUpdate.status().get()));
            } else {
                updates.add(set(FIELD_STATUS, JobTriggerStatus.RUNNABLE));
            }
            updates.add(set(FIELD_NEXT_TIME, triggerUpdate.nextTime().get()));
        } else {
            updates.add(set(FIELD_STATUS, triggerUpdate.status().orElse(JobTriggerStatus.COMPLETE)));
        }

        if (triggerUpdate.data().isPresent()) {
            updates.add(set(FIELD_DATA, triggerUpdate.data()));
        }
        trigger.triggeredAt().ifPresent(triggeredAt -> {
            var duration = new org.joda.time.Duration(triggeredAt, clock.nowUTC());
            updates.add(set(FIELD_LAST_EXECUTION_DURATION, Optional.of(duration.getMillis())));
        });

        return collection.updateOne(filter, combine(updates)).getModifiedCount() == 1;
    }

    /**
     * <strong>WARNING:</strong> This should <em>only</em> be called before the job scheduler is started! Calling this
     * while the scheduler is running might result in data corruption or inconsistencies!
     * <p></p>
     * This method will release all triggers that are locked by the calling node. It should be called before starting
     * the scheduler service on the current node to release all triggers that might be in a stale
     * {@link JobTriggerStatus#RUNNING RUNNING} status after an unclean JVM or Graylog server shutdown.
     *
     * @return number of released triggers
     */
    public int forceReleaseOwnedTriggers() {
        final var filter = and(
                // Only select trigger for force release which are owned by the calling node
                eq(FIELD_LOCK_OWNER, nodeId),
                eq(FIELD_STATUS, JobTriggerStatus.RUNNING)
        );
        final var update = combine(
                unset(FIELD_LOCK_OWNER),
                set(FIELD_STATUS, JobTriggerStatus.RUNNABLE));

        return (int) collection.updateMany(filter, update).getModifiedCount();
    }

    /**
     * Mark the given trigger as defective to make sure it will not be scheduled anymore.
     *
     * @param trigger trigger that should be marked as defective
     * @return true if the trigger has been modified, false otherwise
     */
    public boolean setTriggerError(JobTriggerDto trigger) {
        requireNonNull(trigger, "trigger cannot be null");

        final var filter = and(
                // Make sure that the owner still owns the trigger
                eq(FIELD_LOCK_OWNER, nodeId),
                eq(FIELD_ID, getId(trigger))
        );
        final var update = combine(
                unset(FIELD_LOCK_OWNER),
                set(FIELD_STATUS, JobTriggerStatus.ERROR));

        return collection.updateOne(filter, update).getModifiedCount() > 0;
    }

    private ObjectId getId(JobTriggerDto trigger) {
        return new ObjectId(requireNonNull(trigger.id(), "trigger ID cannot be null"));
    }

    public void updateLockedJobTriggers() {
        final DateTime now = clock.nowUTC();
        final var filter = and(
                eq(FIELD_LOCK_OWNER, nodeId),
                eq(FIELD_STATUS, JobTriggerStatus.RUNNING)
        );
        collection.updateMany(filter, set(FIELD_LAST_LOCK_TIME, now));
    }

    /**
     * Update the job progress on a trigger.
     *
     * @param trigger  the trigger to update
     * @param progress the job progress in percent (0-100)
     */
    public int updateProgress(JobTriggerDto trigger, int progress) {
        final var filter = eq(FIELD_ID, new ObjectId(requireNonNull(trigger.id())));
        final var update = set(FIELD_PROGRESS, progress);
        return (int) collection.updateOne(filter, update).getModifiedCount();
    }

    /**
     * Cancel a JobTrigger that matches a query.
     *
     * @param query the db query
     * @return an Optional of the trigger that was cancelled. Empty if no matching trigger was found.
     */
    public Optional<JobTriggerDto> cancelTriggerByQuery(Bson query) {
        final var update = set(FIELD_IS_CANCELLED, true);

        return Optional.ofNullable(collection.findOneAndUpdate(query, update));
    }

    @Deprecated
    public Optional<JobTriggerDto> cancelTriggerByQuery(DBQuery.Query query) {
        mongoUtils.initializeLegacyMongoJackBsonObject(query);
        return cancelTriggerByQuery((Bson) query);
    }


    /**
     * Find triggers by using the provided query. Use judiciously!
     *
     * @param query The query
     * @return All found JobTriggers
     */
    public List<JobTriggerDto> findByQuery(Bson query) {
        return stream(collection.find(query).sort(descending(FIELD_UPDATED_AT))).toList();
    }

    @Deprecated
    public List<JobTriggerDto> findByQuery(DBQuery.Query query) {
        mongoUtils.initializeLegacyMongoJackBsonObject(query);
        return findByQuery((Bson) query);
    }

    private record OverdueTrigger(@JsonProperty("_id") String type, @JsonProperty("count") long count) {}

    /**
     * Returns the number of overdue triggers grouped by job type.
     *
     * @return a map of job type counts
     */
    public Map<String, Long> numberOfOverdueTriggers() {
        final DateTime now = clock.nowUTC();
        final AggregateIterable<OverdueTrigger> result = collection.aggregate(List.of(
                Aggregates.match(
                        // We deliberately don't include the filter to include expired trigger locks we use in
                        // #nextRunnableTrigger because we consider that an edge case that's not important for
                        // the overdue calculation.
                        and(
                                eq(FIELD_LOCK_OWNER, null),
                                eq(FIELD_STATUS, JobTriggerStatus.RUNNABLE),
                                lte(FIELD_NEXT_TIME, now),
                                or(
                                        not(exists(FIELD_END_TIME)),
                                        eq(FIELD_END_TIME, null),
                                        Filters.gte(FIELD_END_TIME, now)
                                )
                        )
                ),
                Aggregates.group(
                        "$" + FIELD_JOB_DEFINITION_TYPE,
                        Accumulators.sum("count", 1)
                )
        ), OverdueTrigger.class);

        return stream(result).collect(Collectors.toMap(OverdueTrigger::type, OverdueTrigger::count));
    }
}
