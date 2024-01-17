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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import jakarta.inject.Inject;

import static org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId.USER_ID_SUB_FIELD;

public class EntityListPreferencesServiceImpl implements EntityListPreferencesService {

    public static final String ENTITY_LIST_PREFERENCES_MONGO_COLLECTION_NAME = "entity_list_preferences";

    private final JacksonDBCollection<StoredEntityListPreferences, StoredEntityListPreferencesId> db;

    @Inject
    public EntityListPreferencesServiceImpl(final MongoConnection mongoConnection,
                                            final MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(ENTITY_LIST_PREFERENCES_MONGO_COLLECTION_NAME),
                StoredEntityListPreferences.class,
                StoredEntityListPreferencesId.class,
                mapper.get(),
                null);

    }

    @Override
    public StoredEntityListPreferences get(final StoredEntityListPreferencesId preferencesId) {
        return this.db.findOneById(preferencesId);
    }

    @Override
    public boolean save(final StoredEntityListPreferences preferences) {
        final WriteResult<StoredEntityListPreferences, StoredEntityListPreferencesId> save = db.save(preferences);
        return save.getWriteResult().getN() > 0;
    }

    @Override
    public int deleteAllForUser(String userId) {
        DBObject query = new BasicDBObject();
        query.put("_id." + USER_ID_SUB_FIELD, userId);
        final WriteResult<StoredEntityListPreferences, StoredEntityListPreferencesId> result = this.db.remove(query);
        return result.getN();
    }
}
