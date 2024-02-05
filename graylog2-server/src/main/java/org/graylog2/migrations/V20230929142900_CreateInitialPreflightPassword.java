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
package org.graylog2.migrations;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.apache.commons.lang.RandomStringUtils;
import org.bson.types.ObjectId;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.security.RestPermissions;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

public class V20230929142900_CreateInitialPreflightPassword extends Migration {
    private final MongoConnection mongoConnection;

    @Inject
    public V20230929142900_CreateInitialPreflightPassword(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-09-29T14:29:00Z");
    }

    @Override
    public MigrationType migrationType() {
        return MigrationType.PREFLIGHT;
    }

    @Override
    public void upgrade() {
        final DBCollection collection = mongoConnection.getDatabase().getCollection("preflight");

        final WriteResult result = collection.update(
                new BasicDBObject("result", new BasicDBObject("$exists", true)),
                new BasicDBObject(Map.of(
                        "$set", new BasicDBObject("type", "preflight_result"),
                        "$rename", new BasicDBObject("result", "value")
                )),
                false,
                true);

        collection.update(
                new BasicDBObject("type", "preflight_password"),
                new BasicDBObject("$setOnInsert", new BasicDBObject(Map.of(
                        "type", "preflight_password",
                        "value", RandomStringUtils.randomAlphabetic(PreflightConstants.INITIAL_PASSWORD_LENGTH)
                ))),
                true,
                false);
    }
}
