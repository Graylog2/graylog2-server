/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.system.processing;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;

import static org.graylog2.system.processing.ProcessingStatusDto.FIELD_UPDATED_AT;

/**
 * Manages the database collection for processing status.
 */
public class DBProcessingStatusService {
    static final String COLLECTION_NAME = "processing_status";
    private static final String FIELD_WRITTEN_MESSAGES_1M = ProcessingStatusDto.FIELD_INPUT_JOURNAL + "." + ProcessingStatusDto.JournalInfo.FIELD_WRITTEN_MESSAGES_1M_RATE;
    private static final String FIELD_UNCOMMITTED_ENTRIES = ProcessingStatusDto.FIELD_INPUT_JOURNAL + "." + ProcessingStatusDto.JournalInfo.FIELD_UNCOMMITTED_ENTRIES;

    private final String nodeId;
    private final JobSchedulerClock clock;
    private final Duration updateThreshold;
    private final double journalWriteRateThreshold;
    private final JacksonDBCollection<ProcessingStatusDto, ObjectId> db;

    @Inject
    public DBProcessingStatusService(MongoConnection mongoConnection,
                                     NodeId nodeId,
                                     JobSchedulerClock clock,
                                     @Named(ProcessingStatusConfig.UPDATE_THRESHOLD) Duration updateThreshold,
                                     @Named(ProcessingStatusConfig.JOURNAL_WRITE_RATE_THRESHOLD) int journalWriteRateThreshold,
                                     MongoJackObjectMapperProvider mapper) {
        this.nodeId = nodeId.toString();
        this.clock = clock;
        this.updateThreshold = updateThreshold;
        this.journalWriteRateThreshold = ((Number) journalWriteRateThreshold).doubleValue();
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ProcessingStatusDto.class,
                ObjectId.class,
                mapper.get());

        db.createIndex(new BasicDBObject(ProcessingStatusDto.FIELD_NODE_ID, 1), new BasicDBObject("unique", true));
        db.createIndex(new BasicDBObject(FIELD_UPDATED_AT, 1)
                .append(FIELD_UNCOMMITTED_ENTRIES, 1)
                .append(FIELD_WRITTEN_MESSAGES_1M, 1));
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
        final DBQuery.Query query = getDataSelectionQuery(clock, updateThreshold, journalWriteRateThreshold);

        // Get the earliest timestamp of the post-indexing receive timestamp by sorting and returning the first one.
        // We use the earliest timestamp because some nodes can be faster than others and we need to make sure
        // to return the timestamp of the slowest one.
        try (DBCursor<ProcessingStatusDto> cursor = db.find(query).sort(DBSort.asc(sortField)).limit(1)) {
            if (cursor.hasNext()) {
                return Optional.of(cursor.next().receiveTimes().postIndexing());
            }
            return Optional.empty();
        }
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
                        DBQuery.greaterThanEquals(FIELD_UNCOMMITTED_ENTRIES, 1L)
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
        return db.findAndModify(
                DBQuery.is(ProcessingStatusDto.FIELD_NODE_ID, nodeId),
                null,
                null,
                false,
                ProcessingStatusDto.of(nodeId, processingStatusRecorder, updatedAt),
                true, // We want to return the updated document to the caller
                true);
    }
}
