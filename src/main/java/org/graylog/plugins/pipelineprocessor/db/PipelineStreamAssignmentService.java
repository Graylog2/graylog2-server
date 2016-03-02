/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.db;

import com.google.common.collect.Sets;
import com.mongodb.MongoException;
import org.graylog.plugins.pipelineprocessor.rest.PipelineStreamAssignment;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public class PipelineStreamAssignmentService {
    private static final Logger log = LoggerFactory.getLogger(PipelineStreamAssignmentService.class);

    public static final String COLLECTION = "pipeline_processor_pipelines_streams";

    private final JacksonDBCollection<PipelineStreamAssignment, String> dbCollection;

    @Inject
    public PipelineStreamAssignmentService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION),
                PipelineStreamAssignment.class,
                String.class,
                mapper.get());
        dbCollection.createIndex(DBSort.asc("stream_id"));
    }


    public PipelineStreamAssignment save(PipelineStreamAssignment assignment) {
        PipelineStreamAssignment existingAssignment = dbCollection.findOne(DBQuery.is("stream_id", assignment.streamId()));
        if (existingAssignment == null) {
            existingAssignment = PipelineStreamAssignment.create(null, assignment.streamId(), Collections.emptySet());
        }

        final PipelineStreamAssignment toSave = existingAssignment.toBuilder()
                .pipelineIds(assignment.pipelineIds()).build();
        final WriteResult<PipelineStreamAssignment, String> save = dbCollection.save(toSave);
        return save.getSavedObject();
    }

    public PipelineStreamAssignment load(String streamId) throws NotFoundException {
        final PipelineStreamAssignment oneById = dbCollection.findOne(DBQuery.is("stream_id", streamId));
        if (oneById == null) {
            throw new NotFoundException("No pipeline assignments with for stream " + streamId);
        }
        return oneById;
    }

    public Set<PipelineStreamAssignment> loadAll() {
        try {
            final DBCursor<PipelineStreamAssignment> assignments = dbCollection.find();
            return Sets.newHashSet(assignments.iterator());
        } catch (MongoException e) {
            log.error("Unable to load pipelines", e);
            return Collections.emptySet();
        }
    }
}
