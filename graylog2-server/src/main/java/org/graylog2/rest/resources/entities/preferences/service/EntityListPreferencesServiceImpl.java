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
package org.graylog2.rest.resources.entities.preferences.service;

import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollections;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId;

import static org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId.USER_ID_SUB_FIELD;

public class EntityListPreferencesServiceImpl implements EntityListPreferencesService {

    public static final String ENTITY_LIST_PREFERENCES_MONGO_COLLECTION_NAME = "entity_list_preferences";

    private final MongoCollection<StoredEntityListPreferences> collection;

    @Inject
    public EntityListPreferencesServiceImpl(MongoCollections mongoCollections) {
        collection = mongoCollections.nonEntityCollection(ENTITY_LIST_PREFERENCES_MONGO_COLLECTION_NAME,
                StoredEntityListPreferences.class);
    }

    @Override
    public StoredEntityListPreferences get(final StoredEntityListPreferencesId preferencesId) {
        return collection.find(Filters.eq("_id", preferencesId)).first();
    }

    @Override
    public boolean save(final StoredEntityListPreferences preferences) {
        if (preferences.preferencesId() == null) {
            collection.insertOne(preferences);
        } else {
            collection.replaceOne(Filters.eq("_id", preferences.preferencesId()), preferences,
                    new ReplaceOptions().upsert(true));
        }
        return true; // ¯\_(ツ)_/¯
    }

    @Override
    public int deleteAllForUser(String userId) {
        return Ints.saturatedCast(collection.deleteMany(Filters.eq("_id." + USER_ID_SUB_FIELD, userId))
                .getDeletedCount());
    }
}
