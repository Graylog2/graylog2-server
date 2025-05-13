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
package org.graylog2.contentstream.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.Optional;

import static org.graylog2.contentstream.db.ContentStreamUserSettings.FIELD_USER_ID;

public class DBContentStreamUserSettingsService {

    public static final String COLLECTION_NAME = "content_stream_user_settings";

    private final MongoCollection<ContentStreamUserSettings> collection;

    @Inject
    public DBContentStreamUserSettingsService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, ContentStreamUserSettings.class);
    }

    public Optional<ContentStreamUserSettings> findByUserId(String userId) {
        return Optional.ofNullable(collection.find(Filters.eq(FIELD_USER_ID, userId)).first());
    }

    public void save(ContentStreamUserSettings dto) {
        if (dto.id() == null) {
            collection.insertOne(dto);
        } else {
            collection.replaceOne(MongoUtils.idEq(dto.id()), dto, new ReplaceOptions().upsert(true));
        }
    }

    public void delete(String userId) {
        collection.deleteMany(Filters.eq(FIELD_USER_ID, userId));
    }
}
