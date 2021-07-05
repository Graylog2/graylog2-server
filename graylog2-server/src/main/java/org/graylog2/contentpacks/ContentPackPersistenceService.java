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
package org.graylog2.contentpacks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.Revisioned;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.streams.StreamService;
import org.jooq.lambda.tuple.Tuple2;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ContentPackPersistenceService {
    public static final String COLLECTION_NAME = "content_packs";

    private final JacksonDBCollection<ContentPack, ObjectId> dbCollection;
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackPersistenceService.class);
    private final StreamService streamService;

    @Inject
    public ContentPackPersistenceService(final MongoJackObjectMapperProvider mapperProvider,
                                         final MongoConnection mongoConnection, final StreamService streamService) {
        this(JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ContentPack.class, ObjectId.class, mapperProvider.get()), streamService);
    }

    ContentPackPersistenceService(final JacksonDBCollection<ContentPack, ObjectId> dbCollection, final StreamService streamService) {
        this.dbCollection = dbCollection;
        this.streamService = streamService;

        try {
            dbCollection.createIndex(new BasicDBObject(Identified.FIELD_META_ID, 1).append(Revisioned.FIELD_META_REVISION, 1), new BasicDBObject("unique", true));
        } catch (DuplicateKeyException e) {
            // Ignore - this can happen if this runs before the migration of old content packs
        }
    }

    public Set<ContentPack> loadAll() {
        final DBCursor<ContentPack> contentPacks = dbCollection.find();
        return ImmutableSet.copyOf((Iterable<ContentPack>) contentPacks);
    }

    public Set<ContentPack> loadAllLatest() {
        final Set<ContentPack> allContentPacks = loadAll();
        final ImmutableMultimap.Builder<ModelId, ContentPack> byIdBuilder = ImmutableMultimap.builder();
        for (ContentPack contentPack : allContentPacks) {
            byIdBuilder.put(contentPack.id(), contentPack);
        }

        final ImmutableMultimap<ModelId, ContentPack> contentPacksById = byIdBuilder.build();
        final ImmutableSet.Builder<ContentPack> latestContentPacks = ImmutableSet.builderWithExpectedSize(contentPacksById.keySet().size());
        for (ModelId id : contentPacksById.keySet()) {
            final ImmutableCollection<ContentPack> contentPacks = contentPacksById.get(id);
            final ContentPack latestContentPackRevision = Collections.max(contentPacks, Comparator.comparingInt(Revisioned::revision));
            latestContentPacks.add(latestContentPackRevision);
        }

        return latestContentPacks.build();
    }

    public Set<ContentPack> findAllById(ModelId id) {
        final DBCursor<ContentPack> result = dbCollection.find(DBQuery.is(Identified.FIELD_META_ID, id));
        return ImmutableSet.copyOf((Iterable<ContentPack>) result);
    }

    public Optional<ContentPack> findByIdAndRevision(ModelId id, int revision) {
        final DBQuery.Query query = DBQuery.is(Identified.FIELD_META_ID, id).is(Revisioned.FIELD_META_REVISION, revision);
        return Optional.ofNullable(dbCollection.findOne(query));
    }

    public Optional<ContentPack> insert(final ContentPack pack) {
        if (findByIdAndRevision(pack.id(), pack.revision()).isPresent()) {
            LOG.debug("Content pack already found: id: {} revision: {}. Did not insert!", pack.id(), pack.revision());
            return Optional.empty();
        }
        final WriteResult<ContentPack, ObjectId> writeResult = dbCollection.insert(pack);
        return Optional.of(writeResult.getSavedObject());
    }

    public int deleteById(ModelId id) {
        final DBQuery.Query query = DBQuery.is(Identified.FIELD_META_ID, id);
        final WriteResult<ContentPack, ObjectId> writeResult = dbCollection.remove(query);
        return writeResult.getN();
    }

    public int deleteByIdAndRevision(ModelId id, int revision) {
        final DBQuery.Query query = DBQuery.is(Identified.FIELD_META_ID, id).is(Revisioned.FIELD_META_REVISION, revision);
        final WriteResult<ContentPack, ObjectId> writeResult = dbCollection.remove(query);
        return writeResult.getN();
    }
}
