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
package org.graylog2.telemetry.user.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.Optional;

import static org.graylog2.telemetry.user.db.TelemetryUserSettingsDto.FIELD_USER_ID;

public class DBTelemetryUserSettingsService {

    public static final String COLLECTION_NAME = "telemetry_user_settings";

    private final MongoCollection<TelemetryUserSettingsDto> collection;
    private final MongoUtils<TelemetryUserSettingsDto> mongoUtils;

    @Inject
    public DBTelemetryUserSettingsService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, TelemetryUserSettingsDto.class);
        mongoUtils = mongoCollections.utils(collection);
    }

    public Optional<TelemetryUserSettingsDto> findByUserId(String userId) {
        return Optional.ofNullable(collection.find(Filters.eq(FIELD_USER_ID, userId)).first());
    }

    public void save(TelemetryUserSettingsDto dto) {
        mongoUtils.save(dto);
    }

    public void delete(String userId) {
        collection.deleteMany(Filters.eq(FIELD_USER_ID, userId));
    }
}
