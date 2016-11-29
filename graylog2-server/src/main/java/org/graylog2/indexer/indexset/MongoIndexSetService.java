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
package org.graylog2.indexer.indexset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indexset.events.IndexSetDeletedEvent;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MongoIndexSetService implements IndexSetService {
    private static final String COLLECTION_NAME = "index_sets";

    private final JacksonDBCollection<IndexSetConfig, ObjectId> collection;
    private final ClusterEventBus clusterEventBus;
    private final StreamService streamService;

    @Inject
    public MongoIndexSetService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider objectMapperProvider,
                                StreamService streamService,
                                ClusterEventBus clusterEventBus) {
        this(JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                IndexSetConfig.class,
                ObjectId.class,
                objectMapperProvider.get()),
                streamService,
                clusterEventBus);
    }

    @VisibleForTesting
    protected MongoIndexSetService(JacksonDBCollection<IndexSetConfig, ObjectId> collection,
                                   StreamService streamService,
                                   ClusterEventBus clusterEventBus) {
        this.collection = requireNonNull(collection);
        this.streamService = streamService;
        this.clusterEventBus = requireNonNull(clusterEventBus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<IndexSetConfig> get(String id) {
        return get(new ObjectId(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<IndexSetConfig> get(ObjectId id) {
        final DBQuery.Query query = DBQuery.is("_id", id);
        final IndexSetConfig indexSetConfig = collection.findOne(query);

        return Optional.ofNullable(indexSetConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<IndexSetConfig> findOne(DBQuery.Query query) {
        return Optional.ofNullable(collection.findOne(query));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexSetConfig> findAll() {
        return ImmutableList.copyOf((Iterator<? extends IndexSetConfig>) collection.find());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexSetConfig> findPaginated(Set<String> indexSetIds, int limit, int skip) {
        final List<DBQuery.Query> idQuery = indexSetIds.stream()
                .map(id -> DBQuery.is("_id", id))
                .collect(Collectors.toList());

        final DBQuery.Query query = DBQuery.or(idQuery.toArray(new DBQuery.Query[0]));

        return ImmutableList.copyOf(collection.find(query)
                .sort(DBSort.desc("creation_date"))
                .skip(skip)
                .limit(limit)
                .toArray());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexSetConfig save(IndexSetConfig indexSetConfig) {
        final WriteResult<IndexSetConfig, ObjectId> writeResult = collection.save(indexSetConfig);
        final IndexSetConfig savedObject = writeResult.getSavedObject();

        final IndexSetCreatedEvent createdEvent = IndexSetCreatedEvent.create(savedObject);
        clusterEventBus.post(createdEvent);

        return savedObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(String id) {
        return delete(new ObjectId(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(ObjectId id) {
        if (!isDeletable(id)) {
            return 0;
        }

        final DBQuery.Query query = DBQuery.is("_id", id);
        final WriteResult<IndexSetConfig, ObjectId> writeResult = collection.remove(query);

        final int removedEntries = writeResult.getN();
        if (removedEntries > 0) {
            final IndexSetDeletedEvent deletedEvent = IndexSetDeletedEvent.create(id.toHexString());
            clusterEventBus.post(deletedEvent);
        }

        return removedEntries;
    }

    private boolean isDeletable(ObjectId id) {
        final String stringId = id.toHexString();

        // TODO: This can be expensive, create a method in StreamService to find streams for a given index set ID.
        for (Stream stream : streamService.loadAll()) {
            if (stream.getIndexSetId() != null && stream.getIndexSetId().equals(stringId)) {
                return false;
            }
        }

        return true;
    }
}
