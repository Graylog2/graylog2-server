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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackMetadata;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.contentpacks.model.ContentPackInstallation.FIELD_CONTENT_PACK_ID;
import static org.graylog2.contentpacks.model.ContentPackInstallation.FIELD_CONTENT_PACK_REVISION;
import static org.graylog2.database.utils.MongoUtils.idEq;

@Singleton
public class ContentPackInstallationPersistenceService {
    public static final String COLLECTION_NAME = "content_packs_installations";

    private final MongoCollection<ContentPackInstallation> collection;

    @Inject
    public ContentPackInstallationPersistenceService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, ContentPackInstallation.class);

        collection.createIndex(Indexes.ascending(FIELD_CONTENT_PACK_ID));
        collection.createIndex(Indexes.ascending(FIELD_CONTENT_PACK_ID, FIELD_CONTENT_PACK_REVISION));
    }

    public Set<ContentPackInstallation> loadAll() {
        return ImmutableSet.copyOf(collection.find());
    }

    public Set<ContentPackInstallation> findByContentPackIds(Set<ModelId> ids) {
        final Set<String> stringIds = ids.stream().map(ModelId::toString).collect(Collectors.toSet());
        return ImmutableSet.copyOf(collection.find(Filters.in(FIELD_CONTENT_PACK_ID, stringIds)));
    }

    public Optional<ContentPackInstallation> findById(ObjectId id) {
        return Optional.ofNullable(collection.find(idEq(id)).first());
    }

    public Set<ContentPackInstallation> findByContentPackIdAndRevision(ModelId id, int revision) {
        final var query = and(
                eq(FIELD_CONTENT_PACK_ID, id),
                eq(FIELD_CONTENT_PACK_REVISION, revision));
        return ImmutableSet.copyOf(collection.find(query));
    }

    public Set<ContentPackInstallation> findByContentPackId(ModelId id) {
        return ImmutableSet.copyOf(collection.find(eq(FIELD_CONTENT_PACK_ID, id)));
    }

    public ContentPackInstallation insert(final ContentPackInstallation installation) {
        final var savedId = MongoUtils.insertedId(collection.insertOne(installation));
        return installation.toBuilder().id(savedId).build();
    }

    public int deleteById(ObjectId id) {
        return (int) collection.deleteOne(idEq(id)).getDeletedCount();
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

        return collection.countDocuments(eq(field, entityId));
    }

    public long countInstallationOfEntityByIdAndFoundOnSystem(ModelId entityId) {
        final var query = Filters.elemMatch(ContentPackInstallation.FIELD_ENTITIES,
                and(
                        eq(NativeEntityDescriptor.FIELD_ENTITY_FOUND_ON_SYSTEM, true),
                        eq(NativeEntityDescriptor.FIELD_META_ID, entityId.id())));

        return collection.countDocuments(query);
    }
}
