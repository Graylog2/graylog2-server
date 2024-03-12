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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.users.RoleServiceImpl;
import org.graylog2.users.UserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import static com.mongodb.client.model.Projections.include;

/**
 * Removes a role.
 * Bypasses all the checks, removes even read-only roles.
 * Because of that it is placed in migrations module, instead of being added to {@link org.graylog2.users.RoleService}
 */
class RoleRemover {

    private static final Logger LOG = LoggerFactory.getLogger(RoleRemover.class);

    private final MongoConnection mongoConnection;

    @Inject
    public RoleRemover(final MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    void removeBuiltinRole(final String roleName) {
        final Bson roleFindingFilter = Filters.eq(RoleServiceImpl.NAME_LOWER, roleName.toLowerCase(Locale.ENGLISH));
        final MongoDatabase mongoDatabase = mongoConnection.getMongoDatabase();
        final MongoCollection<Document> rolesCollection = mongoDatabase.getCollection(RoleServiceImpl.ROLES_COLLECTION_NAME);

        final Document role = rolesCollection.find(roleFindingFilter)
                .projection(include("_id"))
                .first();

        if (role != null) {
            final ObjectId roleToBeRemovedId = role.getObjectId("_id");
            final MongoCollection<Document> usersCollection = mongoDatabase.getCollection(UserImpl.COLLECTION_NAME);
            final UpdateResult updateResult = usersCollection.updateMany(Filters.empty(), Updates.pull(UserImpl.ROLES, roleToBeRemovedId));
            if (updateResult.getModifiedCount() > 0) {
                LOG.info(StringUtils.f("Removed role %s from %d users", roleName, updateResult.getModifiedCount()));
            }
            final DeleteResult deleteResult = rolesCollection.deleteOne(roleFindingFilter);
            if (deleteResult.getDeletedCount() > 0) {
                LOG.info(StringUtils.f("Removed role %s ", roleName));
            } else {
                LOG.warn(StringUtils.f("Failed to remove role %s migration!", roleName));
            }
        }
    }
}
