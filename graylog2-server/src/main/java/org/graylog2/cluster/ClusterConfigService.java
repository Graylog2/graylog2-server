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
package org.graylog2.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClusterConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterConfigService.class);

    @VisibleForTesting
    static final String COLLECTION_NAME = "cluster_config";

    private final JacksonDBCollection<ClusterConfig, String> dbCollection;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;
    private final EventBus clusterEventBus;

    @Inject
    public ClusterConfigService(final MongoJackObjectMapperProvider mapperProvider,
                                final MongoConnection mongoConnection,
                                final NodeId nodeId,
                                final ObjectMapper objectMapper,
                                @ClusterEventBus final EventBus clusterEventBus) {
        this(JacksonDBCollection.wrap(prepareCollection(mongoConnection), ClusterConfig.class, String.class, mapperProvider.get()),
                nodeId, objectMapper, clusterEventBus);
    }

    ClusterConfigService(final JacksonDBCollection<ClusterConfig, String> dbCollection,
                         final NodeId nodeId,
                         final ObjectMapper objectMapper,
                         final EventBus clusterEventBus) {
        this.nodeId = checkNotNull(nodeId);
        this.dbCollection = checkNotNull(dbCollection);
        this.objectMapper = checkNotNull(objectMapper);
        this.clusterEventBus = checkNotNull(clusterEventBus);
    }

    @VisibleForTesting
    static DBCollection prepareCollection(final MongoConnection mongoConnection) {
        DBCollection coll = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        coll.createIndex(DBSort.asc("type"), "unique_type", true);
        coll.setWriteConcern(WriteConcern.MAJORITY);

        return coll;
    }

    private <T> T extractPayload(Object payload, Class<T> type) {
        try {
            return objectMapper.convertValue(payload, type);
        } catch (IllegalArgumentException e) {
            LOG.debug("Error while deserializing payload", e);
            return null;
        }
    }

    public <T> T get(Class<T> type) {
        ClusterConfig config = dbCollection.findOne(DBQuery.is("type", type.getCanonicalName()));

        if(config == null) {
            LOG.warn("Couldn't find cluster config of type {}", type.getCanonicalName());
            return null;
        }

        T result = extractPayload(config.payload(), type);
        if(result == null) {
            LOG.error("Couldn't extract payload from cluster config (type: {})", type.getCanonicalName());
        }

        return result;
    }

    public <T> void write(T payload) {
        if(payload == null) {
            LOG.debug("Payload was null. Skipping.");
            return;
        }

        String canonicalClassName = AutoValueUtils.getCanonicalName(payload.getClass());
        ClusterConfig clusterConfig = ClusterConfig.create(canonicalClassName, payload, nodeId.toString());

        dbCollection.update(DBQuery.is("type", canonicalClassName), clusterConfig, true, false, WriteConcern.MAJORITY);

        ClusterConfigChangedEvent event = ClusterConfigChangedEvent.create(
                DateTime.now(DateTimeZone.UTC), nodeId.toString(), canonicalClassName);
        clusterEventBus.post(event);
    }
}
