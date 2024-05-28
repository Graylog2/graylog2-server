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

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

class ViewService {

    private final MongoCollection<View> db;
    private final MongoUtils<View> mongoUtils;

    @Inject
    ViewService(MongoCollections mongoCollections) {
        this.db = mongoCollections.collection("views", View.class);
        this.mongoUtils = mongoCollections.utils(this.db);
    }

    public ObjectId save(View view) {
        return MongoUtils.insertedId(db.insertOne(view));
    }

    public void remove(ObjectId viewId) {
        mongoUtils.deleteById(viewId);
    }

    long count() {
        return db.countDocuments();
    }
}
