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

import com.mongodb.DBCollection;
import org.graylog2.database.MongoConnection;
import org.graylog2.users.UserImpl;
import org.mongojack.DBSort;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;

public class V20230210102500_UniqueUserMigration extends Migration {
    private final MongoConnection mongoConnection;

    @Inject
    public V20230210102500_UniqueUserMigration(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-02-10T10:25:00Z");
    }

    @Override
    public void upgrade() {
        DBCollection coll = mongoConnection.getDatabase().getCollection(UserImpl.COLLECTION_NAME);

        // NOOP, if index already exists
        coll.createIndex(DBSort.asc(UserImpl.USERNAME), "unique_username", true);
    }
}
