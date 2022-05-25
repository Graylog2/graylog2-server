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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.TransactionBody;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import one.util.streamex.StreamEx;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.DBSort;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;
import static java.util.Objects.requireNonNull;
import static org.graylog.scheduler.JobSchedulerConfiguration.LOCK_EXPIRATION_DURATION;

// This class does NOT use PaginatedDbService because we use the triggers collection for locking and need to handle
// updates very carefully.
public class DBJobTriggerService {
    private final static Logger LOG = LoggerFactory.getLogger(DBJobTriggerService.class);
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

    private final MongoClient mongoClient;
    private final JobSchedulerClock clock;
    private final Duration lockExpirationDuration;
    private final MongoCollection<Document> collection;
    private final ObjectMapper objectMapper;

    @Inject
    public DBJobTriggerService(MongoConnection mongoConnection,
                               MongoJackObjectMapperProvider mapper,
                               NodeId nodeId,
                               JobSchedulerClock clock,
                               ObjectMapperProvider objectMapperProvider,
                               @Named(LOCK_EXPIRATION_DURATION) Duration lockExpirationDuration) {
        this.nodeId = nodeId.toString();
        this.clock = clock;
        this.objectMapper = objectMapperProvider.get();
        this.lockExpirationDuration = lockExpirationDuration;
        this.mongoClient = (MongoClient) mongoConnection.connect();
        collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        // TODO
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

        final Query query = DBQuery.is(FIELD_JOB_DEFINITION_ID, jobDefinitionId);

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

        final Query query = DBQuery.in(FIELD_JOB_DEFINITION_ID, queryValues);
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
        final Query query = DBQuery.and(
                DBQuery.is(FIELD_LOCK_OWNER, null),
                DBQuery.is(FIELD_STATUS, JobTriggerStatus.COMPLETE),
                DBQuery.is(FIELD_SCHEDULE + "." + JobSchedule.TYPE_FIELD, OnceJobSchedule.TYPE_NAME),
                DBQuery.lessThan(FIELD_UPDATED_AT, clock.nowUTC().minus(unit.toMillis(timeValue)))
        );
        return db.remove(query).getN();
    }

    /**
     * Deletes job triggers using the given query. <em>Use judiciously</em>, as will make assumptions about the
     * internal data structure of triggers.
     */
    public int deleteByQuery(Query query) {
        return db.remove(query).getN();
    }

    public long countByQuery(Query query) {
        return db.getCount(query);
    }

    /**
     * Locks and returns the next runnable trigger. The caller needs to take care of releasing the trigger lock.
     *
     * @return next runnable trigger if any exists, an empty {@link Optional} otherwise
     */
    public Optional<JobTriggerDto> nextRunnableTrigger() {
        // use java.util.Date because BSON cannot handle jodatime by default
        final Date now = new Date(clock.nowUTC().getMillis());
        final Date expirationTime = new Date(new DateTime(now).minus(lockExpirationDuration.toMilliseconds()).getMillis());


        final Bson expiredTriggers = and(
                ne(FIELD_LOCK_OWNER, null),
                ne(FIELD_LOCK_OWNER, nodeId),
                eq(FIELD_STATUS, JobTriggerStatus.RUNNING.toString().toLowerCase(Locale.US)), // TODO
                lt(FIELD_LAST_LOCK_TIME, expirationTime)
        );
        final Bson query = or(
                and(
                // We cannot lock a trigger that is already locked by another node
                eq(FIELD_LOCK_OWNER, null),
                eq(FIELD_STATUS, JobTriggerStatus.RUNNABLE.toString().toLowerCase(Locale.US)), // TODO
                lte(FIELD_START_TIME, now),
                or(
                        Filters.exists(FIELD_END_TIME, false),
                        eq(FIELD_END_TIME, null),
                        gt(FIELD_END_TIME, now) //TODO what's the optional doing?
                ),
                // TODO: Using the wall clock time here can be problematic if the node time is off
                //       The scheduler should not lock any new triggers if it detects that its clock is wrong
                lte(FIELD_NEXT_TIME, now)),

                expiredTriggers
        );

        // We want to lock the trigger with the oldest next time
        final Bson sort = Sorts.ascending(FIELD_NEXT_TIME);

        final Bson lockUpdateB = Updates.combine(
                Updates.set(FIELD_LOCK_OWNER, nodeId),
                Updates.set(FIELD_STATUS, JobTriggerStatus.RUNNING.toString().toLowerCase(Locale.US)), // TODO
                Updates.set(FIELD_TRIGGERED_AT, now), // TODO optional.of?
                Updates.set(FIELD_LAST_LOCK_TIME, now)
        );

        final ClientSession clientSession = mongoClient.startSession();

        final TransactionBody<Document> transactionBody = () -> {

            // TODO this might need some more thinking
            Bson runningButNotExpired = and(
                    eq(FIELD_STATUS, JobTriggerStatus.RUNNING.toString().toLowerCase(Locale.US)),
                    not(expiredTriggers));

            final AggregateIterable<Document> count = collection.aggregate(clientSession,
                    ImmutableList.of(
                            //runningButNotExpired,
                            //Aggregates.lookup(DBJobDefinitionService.COLLECTION_NAME, FIELD_JOB_DEFINITION_ID, "_id", "job_definition"),
                            Aggregates.group("$" + FIELD_JOB_DEFINITION_ID, Accumulators.sum("count", 1)),
                            Aggregates.project(Projections.computed("job_definition_id", "$toString: $_id")),
                            Aggregates.lookup(DBJobDefinitionService.COLLECTION_NAME, "job_definition_id", "id", "job_definition"),

                            // TODO
                            // TODO This does not work yet. I can't get the pipeline to fill in a default value for max_concurrency
                            // TODO The next pipeline step would be to extract all job_definition_ids that have reached their concurrency limit.
                            // TODO Those job_definition_ids could then be used as a filter in the findOneAndUpdate() query, so we won't pick up
                            // TODO triggers that are beyond their limit.
                            Aggregates.project(Projections.fields(
                                    Projections.include("count", "max_concurrency"),
                                    Projections.computed("max_concurrency", "$ifNull: [ $job_definition.max_concurrency, 1 ]")
                                    ))

                    )
            );
            count.forEach((Consumer<? super Document>) doc -> LOG.info("doc {}", doc.toJson()));

            // Atomically update, lock and return the next runnable trigger
            final Document trigger = collection.findOneAndUpdate(clientSession,
                    query,
                    lockUpdateB,
                    new FindOneAndUpdateOptions()
                            .sort(sort)
                            .returnDocument(ReturnDocument.AFTER) // We need the modified object, so we have access to the lock information
                            .upsert(false));

            return trigger;
        };

        final TransactionOptions txnOptions = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

        try {
            //return Optional.ofNullable(transactionBody.execute());
            final Document triggerDoc = clientSession.withTransaction(transactionBody, txnOptions);
            final JobTriggerDto jobTriggerDto = objectMapper.convertValue(triggerDoc, JobTriggerDto.class);
            return Optional.ofNullable(jobTriggerDto);
        } catch (RuntimeException e) {
            return Optional.empty();
        } finally {
            clientSession.close();
        }

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

        final Query query = DBQuery.and(
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
        final Query query = DBQuery.and(
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

        final Query query = DBQuery.and(
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

    public void updateLockedJobTriggers() {
        final DateTime now = clock.nowUTC();
        Query query = DBQuery.and(
                DBQuery.is(FIELD_LOCK_OWNER, nodeId),
                DBQuery.is(FIELD_STATUS, JobTriggerStatus.RUNNING)
        );
        DBUpdate.Builder update = DBUpdate.set(FIELD_LAST_LOCK_TIME, now);
        db.updateMulti(query, update);
    }
}
