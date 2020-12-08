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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.google.common.collect.Streams;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.stream.Stream;

class DashboardsService {
    private static final String COLLECTION_NAME = "dashboards";
    private final JacksonDBCollection<Dashboard, ObjectId> db;

    @Inject
    DashboardsService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                Dashboard.class,
                ObjectId.class,
                mapper.get());
    }

    Stream<Dashboard> streamAll() {
        final DBCursor<Dashboard> cursor = db.find(DBQuery.empty());
        return Streams.stream((Iterable<Dashboard>) cursor).onClose(cursor::close);
    }
}
