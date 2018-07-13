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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
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

public class MongoDbPipelineStreamConnectionsService implements PipelineStreamConnectionsService {
    private static final Logger log = LoggerFactory.getLogger(MongoDbPipelineStreamConnectionsService.class);

    private static final String COLLECTION = "pipeline_processor_pipelines_streams";

    private final JacksonDBCollection<PipelineConnections, String> dbCollection;
    private final ClusterEventBus clusterBus;

    @Inject
    public MongoDbPipelineStreamConnectionsService(MongoConnection mongoConnection,
                                                   MongoJackObjectMapperProvider mapper,
                                                   ClusterEventBus clusterBus) {
        this.dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION),
                PipelineConnections.class,
                String.class,
                mapper.get());
        this.clusterBus = clusterBus;
        dbCollection.createIndex(DBSort.asc("stream_id"), new BasicDBObject("unique", true));
    }

    @Override
    public PipelineConnections save(PipelineConnections connections) {
        PipelineConnections existingConnections = dbCollection.findOne(DBQuery.is("stream_id", connections.streamId()));
        if (existingConnections == null) {
            existingConnections = PipelineConnections.create(null, connections.streamId(), Collections.emptySet());
        }

        final PipelineConnections toSave = existingConnections.toBuilder()
                .pipelineIds(connections.pipelineIds()).build();
        final WriteResult<PipelineConnections, String> save = dbCollection.save(toSave);

        final PipelineConnections savedConnections = save.getSavedObject();
        clusterBus.post(PipelineConnectionsChangedEvent.create(savedConnections.streamId(), savedConnections.pipelineIds()));

        return savedConnections;
    }

    @Override
    public PipelineConnections load(String streamId) throws NotFoundException {
        final PipelineConnections oneById = dbCollection.findOne(DBQuery.is("stream_id", streamId));
        if (oneById == null) {
            throw new NotFoundException("No pipeline connections with for stream " + streamId);
        }
        return oneById;
    }

    @Override
    public Set<PipelineConnections> loadAll() {
        try (DBCursor<PipelineConnections> connections = dbCollection.find()) {
            return ImmutableSet.copyOf((Iterable<PipelineConnections>) connections);
        } catch (MongoException e) {
            log.error("Unable to load pipeline connections", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<PipelineConnections> loadByPipelineId(String pipelineId) {
        // Thanks, MongoJack!
        // https://github.com/mongojack/mongojack/issues/12
        final DBObject query = new BasicDBObject("pipeline_ids", new BasicDBObject("$in", Collections.singleton(pipelineId)));
        try (DBCursor<PipelineConnections> pipelineConnections = dbCollection.find(query)) {
            return ImmutableSet.copyOf((Iterable<PipelineConnections>) pipelineConnections);
        } catch (MongoException e) {
            log.error("Unable to load pipeline connections for pipeline ID " + pipelineId, e);
            return Collections.emptySet();
        }
    }

    @Override
    public void delete(String streamId) {
        try {
            final PipelineConnections connections = load(streamId);
            final Set<String> pipelineIds = connections.pipelineIds();

            dbCollection.removeById(connections.id());
            clusterBus.post(PipelineConnectionsChangedEvent.create(streamId, pipelineIds));
        } catch (NotFoundException e) {
            log.debug("No connections found for stream " + streamId);
        }
    }
}
