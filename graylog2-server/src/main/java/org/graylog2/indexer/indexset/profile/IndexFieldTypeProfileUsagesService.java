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
package org.graylog2.indexer.indexset.profile;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.MongoIndexSetService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service returning usages of profiles - documents in Mongo that reference profiles.
 * In current states profiles are only used in index sets. If it ever changed, this class would need to be changed significantly as well.
 */
public class IndexFieldTypeProfileUsagesService {

    public static final String INDEX_SET_ID = "_id";
    private final MongoCollection<Document> indexSetsCollection;

    @Inject
    public IndexFieldTypeProfileUsagesService(final MongoConnection mongoConnection) {
        indexSetsCollection = mongoConnection.getMongoDatabase().getCollection(MongoIndexSetService.COLLECTION_NAME);
    }

    public Set<String> usagesOfProfile(final String profileId) {
        Set<String> usagesInIndexSet = new HashSet<>();
        indexSetsCollection
                .find(Filters.eq(IndexSetConfig.FIELD_PROFILE_ID, profileId))
                .projection(Projections.include(INDEX_SET_ID))
                .map(document -> document.getObjectId(INDEX_SET_ID).toString())
                .into(usagesInIndexSet);
        return usagesInIndexSet;
    }

    public Map<String, Set<String>> usagesOfProfiles(final Set<String> profilesIds) {
        Map<String, Set<String>> usagesInIndexSet = new HashMap<>();
        profilesIds.forEach(profId -> usagesInIndexSet.put(profId, new HashSet<>()));
        indexSetsCollection
                .find(Filters.in(IndexSetConfig.FIELD_PROFILE_ID, profilesIds))
                .projection(Projections.include(INDEX_SET_ID, IndexSetConfig.FIELD_PROFILE_ID))
                .forEach(document -> {
                    final String indexSetId = document.getObjectId(INDEX_SET_ID).toString();
                    final String profileId = document.getString(IndexSetConfig.FIELD_PROFILE_ID);
                    usagesInIndexSet.get(profileId).add(indexSetId);
                });

        return usagesInIndexSet;
    }
}
