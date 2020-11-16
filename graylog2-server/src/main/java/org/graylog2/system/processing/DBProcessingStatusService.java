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
package org.graylog2.system.processing;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import org.bson.types.ObjectId;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoDBUpsertRetryer;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.graylog2.system.processing.ProcessingStatusDto.FIELD_UPDATED_AT;

/**
 * Manages the database collection for processing status.
 */
public class DBProcessingStatusService {
    public static final String COLLECTION_NAME = "processing_status";
    private static final String FIELD_WRITTEN_MESSAGES_1M = ProcessingStatusDto.FIELD_INPUT_JOURNAL + "." + ProcessingStatusDto.JournalInfo.FIELD_WRITTEN_MESSAGES_1M_RATE;
    private static final String FIELD_UNCOMMITTED_ENTRIES = ProcessingStatusDto.FIELD_INPUT_JOURNAL + "." + ProcessingStatusDto.JournalInfo.FIELD_UNCOMMITTED_ENTRIES;
    private static final String FIELD_JOURNAL_ENABLED = ProcessingStatusDto.FIELD_INPUT_JOURNAL + "." + ProcessingStatusDto.JournalInfo.FIELD_JOURNAL_ENABLED;

    private final String nodeId;
    private final JobSchedulerClock clock;
    private final Duration updateThreshold;
    private final double journalWriteRateThreshold;
    private final JacksonDBCollection<ProcessingStatusDto, ObjectId> db;
    private final BaseConfiguration baseConfiguration;

    @Inject
    public DBProcessingStatusService(MongoConnection mongoConnection,
                                     NodeId nodeId,
                                     JobSchedulerClock clock,
                                     @Named(ProcessingStatusConfig.UPDATE_THRESHOLD) Duration updateThreshold,
                                     @Named(ProcessingStatusConfig.JOURNAL_WRITE_RATE_THRESHOLD) int journalWriteRateThreshold,
                                     MongoJackObjectMapperProvider mapper,
                                     BaseConfiguration baseConfiguration) {
        this.nodeId = nodeId.toString();
        this.clock = clock;
        this.updateThreshold = updateThreshold;
        this.journalWriteRateThreshold = ((Number) journalWriteRateThreshold).doubleValue();
        this.baseConfiguration = baseConfiguration;
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ProcessingStatusDto.class,
                ObjectId.class,
                mapper.get());

        db.createIndex(new BasicDBObject(ProcessingStatusDto.FIELD_NODE_ID, 1), new BasicDBObject("unique", true));

        // Remove the old (3.1.0) index before creating the new one. This is needed, because mongodb >= 4.2 won't allow
        // the creation of identical indices with a different name. We don't use a migration,
        // because it can race with the code below that creates the same index with a shorter name.
        // TODO remove this in a future release (maybe at 3.5)
        final String OLD_INDEX_NAME = "updated_at_1_input_journal.uncommitted_entries_1_input_journal.written_messages_1m_rate_1";
        try {
            if (db.getIndexInfo().stream().anyMatch(dbo -> dbo.get("name").equals(OLD_INDEX_NAME))) {
                db.dropIndex(OLD_INDEX_NAME);
            }
        } catch (MongoException ignored) {
            // index was either never created or already deleted
        }

        // Use a custom index name to avoid the automatically generated index name which will be pretty long and
        // might cause errors due to the 127 character index name limit. (e.g. when using a long database name)
        // See: https://github.com/Graylog2/graylog2-server/issues/6322
        db.createIndex(new BasicDBObject(FIELD_UPDATED_AT, 1)
                .append(FIELD_UNCOMMITTED_ENTRIES, 1)
                .append(FIELD_WRITTEN_MESSAGES_1M, 1), new BasicDBObject("name", "compound_0"));
    }

    /**
     * Rerturns all existing processing status entries from the database.
     *
     * @return a list of all processing status entries
     */
    public List<ProcessingStatusDto> all() {
        return ImmutableList.copyOf(db.find().sort(DBSort.asc("_id")).iterator());
    }

    /**
     * Returns the processing status entry for the calling node.
     *
     * @return the processing status entry or an empty optional if none exists
     */
    public Optional<ProcessingStatusDto> get() {
        return Optional.ofNullable(db.findOne(DBQuery.is(ProcessingStatusDto.FIELD_NODE_ID, nodeId)));
    }

    /**
     * Returns the earliest post-indexing receive timestamp of all active Graylog nodes in the cluster.
     * This can be used to find out if a certain timerange is already searchable in Elasticsearch.
     * <p>
     * Beware: This only takes the message receive time into account. It doesn't help when log sources send their
     * messages late.
     *
     * @return earliest post-indexing timestamp or empty optional if no processing status entries exist
     */
    public Optional<DateTime> earliestPostIndexingTimestamp() {
        final String sortField = ProcessingStatusDto.FIELD_RECEIVE_TIMES + "." + ProcessingStatusDto.ReceiveTimes.FIELD_POST_INDEXING;
        final DateTime updateThresholdTimestamp = clock.nowUTC().minus(updateThreshold.toMilliseconds());
        final DBQuery.Query queryWithoutMetrics = DBQuery.greaterThan(FIELD_UPDATED_AT, updateThresholdTimestamp);
        final DBQuery.Query queryWithMetrics = getDataSelectionQuery(clock, updateThreshold, journalWriteRateThreshold);

        // First try to query processing status from nodes that are active (include journal metrics restrictions).
        // If no result is found, query the processing status again, but without weeding out nodes with a low input volume.
        // This prevents to completely stall the event processing if the ingestion volume is too low.
        for (DBQuery.Query query: Arrays.asList(queryWithMetrics, queryWithoutMetrics)) {
            // Get the earliest timestamp of the post-indexing receive timestamp by sorting and returning the first one.
            // We use the earliest timestamp because some nodes can be faster than others and we need to make sure
            // to return the timestamp of the slowest one.
            try (DBCursor<ProcessingStatusDto> cursor = db.find(query).sort(DBSort.asc(sortField)).limit(1)) {
                if (cursor.hasNext()) {
                    return Optional.of(cursor.next().receiveTimes().postIndexing());
                }
            }
        }
        return Optional.empty();
    }

    // This has been put into a static method to simplify testing the processing status selection
    @VisibleForTesting
    static DBQuery.Query getDataSelectionQuery(JobSchedulerClock clock, Duration updateThreshold, double journalWriteRateThreshold) {
        final DateTime updateThresholdTimestamp = clock.nowUTC().minus(updateThreshold.toMilliseconds());

        return DBQuery.and(
                // Only select processing status for a node ...
                // ... that has been updated recently
                DBQuery.greaterThan(FIELD_UPDATED_AT, updateThresholdTimestamp),
                // ... and either ...
                DBQuery.or(
                        // ... received a certain amount of messages in the last minute
                        DBQuery.greaterThanEquals(FIELD_WRITTEN_MESSAGES_1M, journalWriteRateThreshold),
                        // ... or has messages left in the journal
                        DBQuery.greaterThanEquals(FIELD_UNCOMMITTED_ENTRIES, 1L),
                        // ... or has journaling disabled
                        DBQuery.is(FIELD_JOURNAL_ENABLED, false)
                )
        );
    }

    /**
     * Create or update (upsert) a processing status entry for the given {@link ProcessingStatusRecorder} using the
     * caller's node ID.
     *
     * @param processingStatusRecorder the processing recorder object to create/update
     * @return the created/updated entry
     */
    public ProcessingStatusDto save(ProcessingStatusRecorder processingStatusRecorder) {
        return save(processingStatusRecorder, DateTime.now(DateTimeZone.UTC));
    }

    @VisibleForTesting
    ProcessingStatusDto save(ProcessingStatusRecorder processingStatusRecorder, DateTime updatedAt) {
        // TODO: Using a timestamp provided by the node for "updated_at" can be bad if the node clock is skewed.
        //       Ideally we would use MongoDB's "$currentDate" but there doesn't seem to be a way to use that
        //       with mongojack.
        return MongoDBUpsertRetryer.run(() -> db.findAndModify(
                DBQuery.is(ProcessingStatusDto.FIELD_NODE_ID, nodeId),
                null,
                null,
                false,
                ProcessingStatusDto.of(nodeId, processingStatusRecorder, updatedAt, baseConfiguration.isMessageJournalEnabled()),
                true, // We want to return the updated document to the caller
                true));
    }
}
