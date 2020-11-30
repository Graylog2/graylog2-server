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

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;

class SearchService {
    protected final JacksonDBCollection<Search, ObjectId> db;

    @Inject
    SearchService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("searches"),
                Search.class,
                ObjectId.class,
                mapper.get());
    }

    public ObjectId save(Search search) {
        return db.insert(search).getSavedId();
    }

    public void remove(ObjectId searchID) { db.removeById(searchID); }

    long count() { return db.count(); }
}
