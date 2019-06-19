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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.List;

public class DBProcessingStatusService {
    private static final String COLLECTION_NAME = "processing_status";

    private final String nodeId;
    private final JacksonDBCollection<ProcessingStatusDto, ObjectId> db;

    @Inject
    public DBProcessingStatusService(MongoConnection mongoConnection,
                                     NodeId nodeId,
                                     MongoJackObjectMapperProvider mapper) {
        this.nodeId = nodeId.toString();
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ProcessingStatusDto.class,
                ObjectId.class,
                mapper.get());

        db.createIndex(new BasicDBObject(ProcessingStatusDto.FIELD_NODE_ID, 1), new BasicDBObject("unique", true));
    }

    public List<ProcessingStatusDto> all() {
        return ImmutableList.copyOf(db.find().sort(DBSort.desc("_id")).iterator());
    }

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
