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
package org.graylog.plugins.views.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static com.mongodb.client.model.Filters.eq;

public class V20191204000000_RemoveLegacyViewsPermissions extends Migration {
    private final MongoDatabase mongoDatabase;

    @Inject
    public V20191204000000_RemoveLegacyViewsPermissions(MongoConnection mongoConnection) {
        this.mongoDatabase = mongoConnection.getMongoDatabase();
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-12-04T00:00:00Z");
    }

    @Override
    public void upgrade() {
        MongoCollection<Document> roles = mongoDatabase.getCollection("roles");

        Document viewsUserRole = roles.findOneAndDelete(eq("name", "Views User"));

        if (viewsUserRole != null) {
            removeRoleFromUsers(viewsUserRole);
        }
    }

    private void removeRoleFromUsers(Document viewsUserRole) {
        MongoCollection<Document> users = mongoDatabase.getCollection("users");
        users.updateMany(
                new Document().append("roles", viewsUserRole.get("_id")),
                new Document().append("$pull", new Document().append("roles", viewsUserRole.get("_id"))));
    }
}
