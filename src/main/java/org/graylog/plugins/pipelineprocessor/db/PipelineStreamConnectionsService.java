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
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import org.graylog.plugins.pipelineprocessor.rest.PipelineStreamConnection;
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

public class PipelineStreamConnectionsService {
    private static final Logger log = LoggerFactory.getLogger(PipelineStreamConnectionsService.class);

    public static final String COLLECTION = "pipeline_processor_pipelines_streams";

    private final JacksonDBCollection<PipelineStreamConnection, String> dbCollection;

    @Inject
    public PipelineStreamConnectionsService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION),
                PipelineStreamConnection.class,
                String.class,
                mapper.get());
        dbCollection.createIndex(DBSort.asc("stream_id"), new BasicDBObject("unique", true));
    }


    public PipelineStreamConnection save(PipelineStreamConnection connections) {
        PipelineStreamConnection existingConnections = dbCollection.findOne(DBQuery.is("stream_id", connections.streamId()));
        if (existingConnections == null) {
            existingConnections = PipelineStreamConnection.create(null, connections.streamId(), Collections.emptySet());
        }

        final PipelineStreamConnection toSave = existingConnections.toBuilder()
                .pipelineIds(connections.pipelineIds()).build();
        final WriteResult<PipelineStreamConnection, String> save = dbCollection.save(toSave);
        return save.getSavedObject();
    }

    public PipelineStreamConnection load(String streamId) throws NotFoundException {
        final PipelineStreamConnection oneById = dbCollection.findOne(DBQuery.is("stream_id", streamId));
        if (oneById == null) {
            throw new NotFoundException("No pipeline connections with for stream " + streamId);
        }
        return oneById;
    }

    public Set<PipelineStreamConnection> loadAll() {
        try {
            final DBCursor<PipelineStreamConnection> connections = dbCollection.find();
            return Sets.newHashSet(connections.iterator());
        } catch (MongoException e) {
            log.error("Unable to load pipeline connections", e);
            return Collections.emptySet();
        }
    }
}
