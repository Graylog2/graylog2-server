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

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.database.MongoConnection;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackMetadata;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ContentPackInstallationPersistenceService {
    private static final String COLLECTION_NAME = "content_packs_installations";

    private final JacksonDBCollection<ContentPackInstallation, ObjectId> dbCollection;

    @Inject
    public ContentPackInstallationPersistenceService(final MongoJackObjectMapperProvider mapperProvider,
                                                     final MongoConnection mongoConnection) {
        this(JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ContentPackInstallation.class, ObjectId.class, mapperProvider.get()));
    }

    ContentPackInstallationPersistenceService(final JacksonDBCollection<ContentPackInstallation, ObjectId> dbCollection) {
        this.dbCollection = dbCollection;

        dbCollection.createIndex(new BasicDBObject(ContentPackInstallation.FIELD_CONTENT_PACK_ID, 1));
        dbCollection.createIndex(new BasicDBObject(ContentPackInstallation.FIELD_CONTENT_PACK_ID, 1).append(ContentPackInstallation.FIELD_CONTENT_PACK_REVISION, 1));
    }

    public Set<ContentPackInstallation> loadAll() {
        try (final DBCursor<ContentPackInstallation> installations = dbCollection.find()) {
            return ImmutableSet.copyOf((Iterator<ContentPackInstallation>) installations);
        }
    }

    public Set<ContentPackInstallation> findByContentPackIds(Set<ModelId> ids) {
        final Set<String> stringIds = ids.stream().map(x -> x.toString()).collect(Collectors.toSet());
        final DBObject query = BasicDBObjectBuilder.start()
                .push(ContentPackInstallation.FIELD_CONTENT_PACK_ID)
                .append("$in", stringIds)
                .get();
        final DBCursor<ContentPackInstallation> result = dbCollection.find(query);
        return ImmutableSet.copyOf((Iterable<ContentPackInstallation>) result);
    }

    public Optional<ContentPackInstallation> findById(ObjectId id) {
        final ContentPackInstallation installation = dbCollection.findOneById(id);
        return Optional.ofNullable(installation);
    }

    public Set<ContentPackInstallation> findByContentPackIdAndRevision(ModelId id, int revision) {
        final DBQuery.Query query = DBQuery
                .is(ContentPackInstallation.FIELD_CONTENT_PACK_ID, id)
                .is(ContentPackInstallation.FIELD_CONTENT_PACK_REVISION, revision);
        try (final DBCursor<ContentPackInstallation> installations = dbCollection.find(query)) {
            return ImmutableSet.copyOf((Iterator<ContentPackInstallation>) installations);
        }
    }

    public Set<ContentPackInstallation> findByContentPackId(ModelId id) {
        final DBQuery.Query query = DBQuery.is(ContentPackInstallation.FIELD_CONTENT_PACK_ID, id);
        try (final DBCursor<ContentPackInstallation> installations = dbCollection.find(query)) {
            return ImmutableSet.copyOf((Iterator<ContentPackInstallation>) installations);
        }
    }

    public ContentPackInstallation insert(final ContentPackInstallation installation) {
        final WriteResult<ContentPackInstallation, ObjectId> writeResult = dbCollection.insert(installation);
        return writeResult.getSavedObject();
    }

    public int deleteById(ObjectId id) {
        final WriteResult<ContentPackInstallation, ObjectId> writeResult = dbCollection.removeById(id);
        return writeResult.getN();
    }

    public Map<ModelId, Map<Integer, ContentPackMetadata>> getInstallationMetadata(Set<ModelId> ids) {
        final Set<ContentPackInstallation> contentPackInstallations = findByContentPackIds(ids);
        Map<ModelId, Map<Integer, ContentPackMetadata>> installationMetaData = new HashMap<>();
        for (ContentPackInstallation installation : contentPackInstallations) {
            Map<Integer, ContentPackMetadata> metadataMap = installationMetaData.get(installation.contentPackId());
            if (metadataMap == null) {
                metadataMap = new HashMap<>();
            }
            ContentPackMetadata metadata = metadataMap.get(installation.contentPackRevision());
            int count = 1;
            if (metadata != null) {
                count = metadata.installationCount() + 1;
            }
            ContentPackMetadata newMetadata = ContentPackMetadata.create(count);
            metadataMap.put(installation.contentPackRevision(), newMetadata);
            installationMetaData.put(installation.contentPackId(), metadataMap);
        }
        return installationMetaData;
    }

    /**
     * Returns the number of installations the given content pack entity ID is used in.
     *
     * @param entityId the native entity ID
     * @return number of installations
     */
    public long countInstallationOfEntityById(ModelId entityId) {
        final String field = String.format(Locale.ROOT, "%s.%s", ContentPackInstallation.FIELD_ENTITIES, NativeEntityDescriptor.FIELD_META_ID);

        return dbCollection.getCount(DBQuery.is(field, entityId));
    }

    public long countInstallationOfEntityByIdAndFoundOnSystem(ModelId entityId) {
        final DBQuery.Query query = DBQuery.elemMatch(ContentPackInstallation.FIELD_ENTITIES,
                DBQuery.and(
                        DBQuery.is(NativeEntityDescriptor.FIELD_ENTITY_FOUND_ON_SYSTEM, true),
                        DBQuery.is(NativeEntityDescriptor.FIELD_META_ID, entityId.id())));

        return dbCollection.getCount(query);
    }
}
