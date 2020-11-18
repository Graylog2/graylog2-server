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
package org.graylog2.indexer.indexset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indexset.events.IndexSetDeletedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class MongoIndexSetService implements IndexSetService {
    private static final String COLLECTION_NAME = "index_sets";

    private final JacksonDBCollection<IndexSetConfig, ObjectId> collection;
    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;
    private final StreamService streamService;

    @Inject
    public MongoIndexSetService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider objectMapperProvider,
                                StreamService streamService,
                                ClusterConfigService clusterConfigService,
                                ClusterEventBus clusterEventBus) {
        this(JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                IndexSetConfig.class,
                ObjectId.class,
                objectMapperProvider.get()),
                streamService,
                clusterConfigService,
                clusterEventBus);
    }

    @VisibleForTesting
    protected MongoIndexSetService(JacksonDBCollection<IndexSetConfig, ObjectId> collection,
                                   StreamService streamService,
                                   ClusterConfigService clusterConfigService,
                                   ClusterEventBus clusterEventBus) {
        this.collection = requireNonNull(collection);
        this.streamService = streamService;
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = requireNonNull(clusterEventBus);

        this.collection.getDbCollection().createIndex(DBSort.asc(IndexSetConfig.FIELD_INDEX_PREFIX), null, true);
        this.collection.getDbCollection().createIndex(DBSort.desc(IndexSetConfig.FIELD_CREATION_DATE));
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

    @Override
    public IndexSetConfig getDefault() {
        final DefaultIndexSetConfig defaultIndexSetConfig = clusterConfigService.get(DefaultIndexSetConfig.class);

        checkState(defaultIndexSetConfig != null, "No default index set configured. This is a bug!");

        final String indexSetId = defaultIndexSetConfig.defaultIndexSetId();
        return get(indexSetId)
                .orElseThrow(() -> new IllegalStateException("Couldn't find default index set <" + indexSetId + ">. This is a bug!"));
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
        return ImmutableList.copyOf((Iterator<? extends IndexSetConfig>) collection.find().sort(DBSort.asc("title")));
    }

    @Override
    public List<IndexSetConfig> findByIds(Set<String> ids) {
        return collection.find(DBQuery.in("_id", ids)).toArray();
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
                .sort(DBSort.asc("title"))
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
        return streamService.loadAllWithIndexSet(id.toHexString()).isEmpty();
    }
}
