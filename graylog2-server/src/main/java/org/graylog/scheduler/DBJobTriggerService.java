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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import one.util.streamex.StreamEx;
import org.bson.types.ObjectId;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
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
import static java.util.Objects.requireNonNull;

// This class does NOT use PaginatedDbService because we use the triggers collection for locking and need to handle
// updates very carefully.
public class DBJobTriggerService {
    static final String COLLECTION_NAME = "scheduler_triggers";
    private static final String FIELD_ID = "_id";
    static final String FIELD_JOB_DEFINITION_ID = JobTriggerDto.FIELD_JOB_DEFINITION_ID;
    private static final String FIELD_LOCK_OWNER = JobTriggerDto.FIELD_LOCK + "." + JobTriggerLock.FIELD_OWNER;
    private static final String FIELD_LAST_LOCK_TIME = JobTriggerDto.FIELD_LOCK + "." + JobTriggerLock.FIELD_LAST_LOCK_TIME;
    private static final String FIELD_NEXT_TIME = JobTriggerDto.FIELD_NEXT_TIME;
    private static final String FIELD_START_TIME = JobTriggerDto.FIELD_START_TIME;
    private static final String FIELD_END_TIME = JobTriggerDto.FIELD_END_TIME;
    private static final String FIELD_STATUS = JobTriggerDto.FIELD_STATUS;
    private static final String FIELD_SCHEDULE = JobTriggerDto.FIELD_SCHEDULE;
    private static final String FIELD_DATA = JobTriggerDto.FIELD_DATA;
    private static final String FIELD_UPDATED_AT = JobTriggerDto.FIELD_UPDATED_AT;
    private static final String FIELD_TRIGGERED_AT = JobTriggerDto.FIELD_TRIGGERED_AT;

    private final String nodeId;
    private final JacksonDBCollection<JobTriggerDto, ObjectId> db;
    private final JobSchedulerClock clock;

    @Inject
    public DBJobTriggerService(MongoConnection mongoConnection,
                               MongoJackObjectMapperProvider mapper,
                               NodeId nodeId,
                               JobSchedulerClock clock) {
        this.nodeId = nodeId.toString();
        this.clock = clock;
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                JobTriggerDto.class,
                ObjectId.class,
                mapper.get());

        db.createIndex(new BasicDBObject(FIELD_JOB_DEFINITION_ID, 1));
        db.createIndex(new BasicDBObject(FIELD_LOCK_OWNER, 1));
        db.createIndex(new BasicDBObject(FIELD_STATUS, 1));
        db.createIndex(new BasicDBObject(FIELD_START_TIME, 1));
        db.createIndex(new BasicDBObject(FIELD_END_TIME, 1));
        db.createIndex(new BasicDBObject(FIELD_NEXT_TIME, 1));
    }

    /**
     * Loads all existing records and returns them.
     *
     * @return list of records
     */
    public List<JobTriggerDto> all() {
        return ImmutableList.copyOf(db.find().sort(DBSort.desc(FIELD_ID)).iterator());
    }

    /**
     * Loads the record for the given ID.
     *
     * @param id record ID to load
     * @return filled optional when the record exists, an empty optional otherwise
     */
    public Optional<JobTriggerDto> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    /**
     * Returns all job triggers for the given job definition ID.
     *
     * TODO: Don't throw exception when there is more than one trigger for a job definition. (see source code)
     *
     * @param jobDefinitionId the job definition ID
     * @return list of found job triggers
     */
    public List<JobTriggerDto> getForJob(String jobDefinitionId) {
        if (isNullOrEmpty(jobDefinitionId)) {
            throw new IllegalArgumentException("jobDefinitionId cannot be null or empty");
        }

        final DBQuery.Query query = DBQuery.is(FIELD_JOB_DEFINITION_ID, jobDefinitionId);

        try (final DBCursor<JobTriggerDto> cursor = db.find(query)) {
            final ImmutableList<JobTriggerDto> triggers = ImmutableList.copyOf(cursor.iterator());

            // We are currently expecting only one trigger per job definition. This will most probably change in the
            // future once we extend our scheduler usage.
            // TODO: Don't throw exception when there is more than one trigger for a job definition.
            //       To be able to do this, we need some kind of label system to make sure we can differentiate between
            //       automatically created triggers (e.g. by event definition) and manually created ones.
            if (triggers.size() > 1) {
                throw new IllegalStateException("More than one trigger for job definition <" + jobDefinitionId + ">");
            }

            return triggers;
        }
    }

    /**
     * Returns all job triggers for the given job definition IDs, grouped by job definition ID.
     *
     * TODO: Don't throw exception when there is more than one trigger for a job definition. (see source code)
     *
     * @param jobDefinitionIds the job definition IDs
     * @return list of found job triggers
     */
    public Map<String, List<JobTriggerDto>> getForJobs(Collection<String> jobDefinitionIds) {
        if (jobDefinitionIds == null) {
            throw new IllegalArgumentException("jobDefinitionIds cannot be null");
        }

        final Set<String> queryValues = jobDefinitionIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !isNullOrEmpty(id))
                .collect(Collectors.toSet());

        final DBQuery.Query query = DBQuery.in(FIELD_JOB_DEFINITION_ID, queryValues);
        final Map<String, List<JobTriggerDto>> groupedTriggers = StreamEx.of(db.find(query).toArray())
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

        return db.insert(trigger).getSavedObject();
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
        final DBUpdate.Builder update = DBUpdate
                .set(FIELD_START_TIME, trigger.startTime())
                .set(FIELD_NEXT_TIME, trigger.nextTime())
                .set(FIELD_DATA, trigger.data())
                .set(FIELD_UPDATED_AT, clock.nowUTC());

        if (trigger.endTime().isPresent()) {
            update.set(FIELD_END_TIME, trigger.endTime());
        }

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
            toUnset.forEach(update::unset);

            // Then we can set the specific fields.
            scheduleUpdate.get().forEach(update::set);
        }

        return db.update(DBQuery.is(FIELD_ID, getId(trigger)), update).getN() > 0;
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
        return db.remove(DBQuery.is(FIELD_ID, triggerId)).getN() > 0;
    }

    /**
     * Deletes completed {@link OnceJobSchedule} triggers that are older than the provided time
     *
     * @param timeValue the time range of triggers to be removed
     * @param unit      the unit of the provided timeValue
     * @return the number of deleted triggers
     */
    public int deleteCompletedOnceSchedulesOlderThan(long timeValue, TimeUnit unit) {
        final DBQuery.Query query = DBQuery.and(
                DBQuery.is(FIELD_LOCK_OWNER, null),
                DBQuery.is(FIELD_STATUS, JobTriggerStatus.COMPLETE),
                DBQuery.is(FIELD_SCHEDULE + "." + JobSchedule.TYPE_FIELD, OnceJobSchedule.TYPE_NAME),
                DBQuery.lessThan(FIELD_UPDATED_AT, clock.nowUTC().minus(unit.toMillis(timeValue)))
        );
        return db.remove(query).getN();
    }

    /**
     * Locks and returns the next runnable trigger. The caller needs to take care of releasing the trigger lock.
     *
     * @return next runnable trigger if any exists, an empty {@link Optional} otherwise
     */
    public Optional<JobTriggerDto> nextRunnableTrigger() {
        final DateTime now = clock.nowUTC();

        final DBQuery.Query query = DBQuery.and(
                // We cannot lock a trigger that is already locked by another node
                DBQuery.is(FIELD_LOCK_OWNER, null),
                DBQuery.is(FIELD_STATUS, JobTriggerStatus.RUNNABLE),
                DBQuery.lessThanEquals(FIELD_START_TIME, now),
                DBQuery.or( // Skip triggers that have an endTime which is due
                        DBQuery.notExists(FIELD_END_TIME),
                        DBQuery.is(FIELD_END_TIME, null),
                        DBQuery.greaterThan(FIELD_END_TIME, Optional.of(now))
                ),
                // TODO: Using the wall clock time here can be problematic if the node time is off
                //       The scheduler should not lock any new triggers if it detects that its clock is wrong
                DBQuery.lessThanEquals(FIELD_NEXT_TIME, now)
        );

        // We want to lock the trigger with the oldest next time
        final DBSort.SortBuilder sort = DBSort.asc(FIELD_NEXT_TIME);

        final DBUpdate.Builder lockUpdate = DBUpdate.set(FIELD_LOCK_OWNER, nodeId)
                .set(FIELD_STATUS, JobTriggerStatus.RUNNING)
                .set(FIELD_TRIGGERED_AT, Optional.of(now))
                .set(FIELD_LAST_LOCK_TIME, now);

        // Atomically update, lock and return the next runnable trigger
        final JobTriggerDto trigger = db.findAndModify(
                query,
                null,
                sort,
                false,
                lockUpdate,
                true, // We need the modified object so we have access to the lock information
                false
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

        final DBQuery.Query query = DBQuery.and(
                // Make sure that the owner still owns the trigger
                DBQuery.is(FIELD_LOCK_OWNER, nodeId),
                DBQuery.is(FIELD_ID, getId(trigger)),
                // Only release running triggers. The trigger might have been paused while the trigger was running
                // so we don't want to set it to RUNNABLE again.
                // TODO: This is an issue. If a user set it to PAUSED, we will not unlock it. Figure something out.
                //       Maybe a manual trigger pause will set "nextStatus" if the trigger is currently running?
                //       That next status would need to be set on release.
                DBQuery.is(FIELD_STATUS, JobTriggerStatus.RUNNING)
        );
        final DBUpdate.Builder update = DBUpdate.set(FIELD_LOCK_OWNER, null);

        // An empty next time indicates that this trigger should not be fired anymore. (e.g. for "once" schedules)
        if (triggerUpdate.nextTime().isPresent()) {
            if (triggerUpdate.status().isPresent()) {
                update.set(FIELD_STATUS, triggerUpdate.status().get());
            } else {
                update.set(FIELD_STATUS, JobTriggerStatus.RUNNABLE);
            }
            update.set(FIELD_NEXT_TIME, triggerUpdate.nextTime().get());
        } else {
            update.set(FIELD_STATUS, JobTriggerStatus.COMPLETE);
        }

        if (triggerUpdate.data().isPresent()) {
            update.set(FIELD_DATA, triggerUpdate.data());
        }

        final int changedDocs = db.update(query, update).getN();
        if (changedDocs > 1) {
            throw new IllegalStateException("Expected to release only one trigger (id=" + trigger.id() + ") but database query modified " + changedDocs);
        }
        return changedDocs == 1;
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
        final DBQuery.Query query = DBQuery.and(
                // Only select trigger for force release which are owned by the calling node
                DBQuery.is(FIELD_LOCK_OWNER, nodeId),
                DBQuery.is(FIELD_STATUS, JobTriggerStatus.RUNNING)
        );
        final DBUpdate.Builder update = DBUpdate.set(FIELD_LOCK_OWNER, null)
                .set(FIELD_STATUS, JobTriggerStatus.RUNNABLE);

        return db.updateMulti(query, update).getN();
    }

    /**
     * Mark the given trigger as defective to make sure it will not be scheduled anymore.
     *
     * @param trigger trigger that should be marked as defective
     * @return true if the trigger has been modified, false otherwise
     */
    public boolean setTriggerError(JobTriggerDto trigger) {
        requireNonNull(trigger, "trigger cannot be null");

        final DBQuery.Query query = DBQuery.and(
                // Make sure that the owner still owns the trigger
                DBQuery.is(FIELD_LOCK_OWNER, nodeId),
                DBQuery.is(FIELD_ID, getId(trigger))
        );
        final DBUpdate.Builder update = DBUpdate.set(FIELD_LOCK_OWNER, null)
                .set(FIELD_STATUS, JobTriggerStatus.ERROR);

        return db.update(query, update).getN() > 0;
    }

    private ObjectId getId(JobTriggerDto trigger) {
        return new ObjectId(requireNonNull(trigger.id(), "trigger ID cannot be null"));
    }
}
