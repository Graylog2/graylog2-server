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

import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indexset.events.IndexSetDeletedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.streams.StreamService;
import org.mongojack.DBQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.indexer.indexset.SimpleIndexSetConfig.FIELD_CREATION_DATE;
import static org.graylog2.indexer.indexset.SimpleIndexSetConfig.FIELD_INDEX_PREFIX;
import static org.graylog2.indexer.indexset.SimpleIndexSetConfig.FIELD_PROFILE_ID;

public class MongoIndexSetService implements IndexSetService {
    public static final String COLLECTION_NAME = "index_sets";
    public static final String FIELD_TITLE = "title";

    private final MongoCollection<IndexSetConfig> collection;
    private final MongoUtils<IndexSetConfig> mongoUtils;
    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;
    private final StreamService streamService;

    @Inject
    public MongoIndexSetService(MongoCollections mongoCollections,
                                StreamService streamService,
                                ClusterConfigService clusterConfigService,
                                ClusterEventBus clusterEventBus) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, IndexSetConfig.class);
        this.mongoUtils = mongoCollections.utils(this.collection);
        this.streamService = streamService;
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = requireNonNull(clusterEventBus);

        this.collection.createIndex(Indexes.ascending(FIELD_INDEX_PREFIX), new IndexOptions().unique(true));
        this.collection.createIndex(Indexes.descending(FIELD_CREATION_DATE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<IndexSetConfig> get(String id) {
        return mongoUtils.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<IndexSetConfig> get(ObjectId id) {
        return mongoUtils.getById(id);
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
        mongoUtils.initializeLegacyMongoJackBsonObject(query);
        return Optional.ofNullable(collection.find(query).first());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexSetConfig> findAll() {
        return ImmutableList.copyOf(collection.find().sort(Sorts.ascending(FIELD_TITLE)));
    }

    @Override
    public List<IndexSetConfig> findByIds(Set<String> ids) {
        return collection.find(MongoUtils.stringIdsIn(ids)).into(new ArrayList<>());
    }

    @Override
    public List<IndexSetConfig> findMany(DBQuery.Query query) {
        mongoUtils.initializeLegacyMongoJackBsonObject(query);
        return ImmutableList.copyOf(collection.find(query).sort(Sorts.ascending(FIELD_TITLE)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexSetConfig> findPaginated(Set<String> ids, int limit, int skip) {
        return ImmutableList.copyOf(collection.find(MongoUtils.stringIdsIn(ids))
                .sort(Sorts.ascending(FIELD_TITLE))
                .skip(skip)
                .limit(limit));
    }

    @Override
    public List<IndexSetConfig> searchByTitle(String searchString) {
        String formatedSearchString = String.format(Locale.getDefault(), ".*%s.*", searchString);
        Pattern searchPattern = Pattern.compile(formatedSearchString, Pattern.CASE_INSENSITIVE);

        return ImmutableList.copyOf(collection.find(Filters.regex(FIELD_TITLE, searchPattern))
                .sort(Sorts.ascending(FIELD_TITLE)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexSetConfig save(IndexSetConfig indexSetConfig) {
        String id = indexSetConfig.id();
        if (id != null) {
            collection.replaceOne(idEq(id), indexSetConfig, new ReplaceOptions().upsert(true));
        } else {
            final InsertOneResult insertOneResult = collection.insertOne(indexSetConfig);
            id = MongoUtils.insertedIdAsString(insertOneResult);
        }

        final IndexSetConfig savedObject = indexSetConfig.toBuilder().id(id).build();
        clusterEventBus.post(IndexSetCreatedEvent.create(savedObject));
        return savedObject;
    }

    @Override
    public void removeReferencesToProfile(final String profileId) {
        collection.updateMany(
                Filters.eq(FIELD_PROFILE_ID, profileId),
                Updates.unset(FIELD_PROFILE_ID),
                new UpdateOptions().upsert(false));
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
        int removedEntries = mongoUtils.deleteById(id) ? 1 : 0;
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
