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

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import jakarta.inject.Inject;

import java.util.Optional;

import static org.graylog2.telemetry.user.db.TelemetryUserSettingsDto.FIELD_USER_ID;

public class DBContentStreamUserSettingsService {

    public static final String COLLECTION_NAME = "content_stream_user_settings";

    private final JacksonDBCollection<ContentStreamUserSettings, ObjectId> db;

    @Inject
    public DBContentStreamUserSettingsService(MongoConnection mongoConnection,
                                              MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ContentStreamUserSettings.class,
                ObjectId.class,
                mapper.get());
    }

    public Optional<ContentStreamUserSettings> findByUserId(String userId) {
        return Optional.ofNullable(db.findOne(DBQuery.is(FIELD_USER_ID, userId)));
    }

    public void save(ContentStreamUserSettings dto) {
        db.save(dto);
    }

    public void delete(String userId) {
        db.remove(DBQuery.is(FIELD_USER_ID, userId));
    }
}
