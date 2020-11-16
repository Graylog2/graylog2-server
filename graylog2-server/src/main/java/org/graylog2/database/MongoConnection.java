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
package org.graylog2.database;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.client.MongoDatabase;

public interface MongoConnection {
    Mongo connect();

    /**
     * Get instance of the configured MongoDB database.
     *
     * @return The configured MongoDB database.
     * @deprecated Use {@link #getMongoDatabase()}.
     */
    @Deprecated
    DB getDatabase();

    /**
     * Get instance of the configured MongoDB database.
     *
     * @return The configured MongoDB database.
     */
    MongoDatabase getMongoDatabase();
}
