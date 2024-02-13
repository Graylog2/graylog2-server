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
package org.graylog.plugins.views.startpage.lastOpened;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.startpage.recentActivities.ActivityType;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityEvent;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.users.events.UserDeletedEvent;
import org.mongojack.DBQuery;
import org.mongojack.WriteResult;

import jakarta.inject.Inject;

import java.util.Optional;

public class LastOpenedService extends PaginatedDbService<LastOpenedForUserDTO> {
    public static final String COLLECTION_NAME = "last_opened";


    @Inject
    public LastOpenedService(MongoConnection mongoConnection,
                             MongoJackObjectMapperProvider mapper,
                             EventBus eventBus) {
        super(mongoConnection, mapper, LastOpenedForUserDTO.class, COLLECTION_NAME);
        eventBus.register(this);

        db.createIndex(new BasicDBObject(LastOpenedForUserDTO.FIELD_USER_ID, 1));
        db.createIndex(new BasicDBObject(LastOpenedForUserDTO.FIELD_ITEMS + "." + LastOpenedDTO.FIELD_GRN, 1));
    }

    public Optional<LastOpenedForUserDTO> findForUser(final SearchUser searchUser) {
        return findForUser(searchUser.getUser().getId());
    }

    Optional<LastOpenedForUserDTO> findForUser(final String userId) {
        return streamQuery(DBQuery.is(LastOpenedForUserDTO.FIELD_USER_ID, userId)).findAny();
    }

    public Optional<LastOpenedForUserDTO> create(final LastOpenedForUserDTO lastOpenedItems, final SearchUser searchUser) {
        try {
            final WriteResult<LastOpenedForUserDTO, ObjectId> result = db.insert(lastOpenedItems);
            final LastOpenedForUserDTO savedObject = result.getSavedObject();
            return Optional.ofNullable(savedObject);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to create a last opened collection, collection with this id already exists : " + lastOpenedItems.id());
        }
    }

    @Subscribe
    public void removeLastOpenedOnEntityDeletion(final RecentActivityEvent event) {
        // if an entity is deleted, we can no longer see it in the lastOpened collection
        if (event.activityType().equals(ActivityType.DELETE)) {
            final var grn = event.grn().toString();
            final var query = new BasicDBObject(LastOpenedForUserDTO.FIELD_ITEMS + "." + LastOpenedDTO.FIELD_GRN, grn);
            final var modifications = new BasicDBObject("$pull", new BasicDBObject(LastOpenedForUserDTO.FIELD_ITEMS, new BasicDBObject(LastOpenedDTO.FIELD_GRN, grn)));
            db.updateMulti(query, modifications);
        }
    }

    @Subscribe
    public void removeFavoriteEntityOnUserDeletion(final UserDeletedEvent event) {
        db.remove(DBQuery.is(LastOpenedForUserDTO.FIELD_USER_ID, event.userId()));
    }
}
