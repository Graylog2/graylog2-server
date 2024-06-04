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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.startpage.recentActivities.ActivityType;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityEvent;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.users.events.UserDeletedEvent;

import java.util.Optional;

public class LastOpenedService {
    public static final String COLLECTION_NAME = "last_opened";
    private final MongoCollection<LastOpenedForUserDTO> db;

    @Inject
    public LastOpenedService(MongoCollections mongoCollections,
                             EventBus eventBus) {
        this.db = mongoCollections.collection(COLLECTION_NAME, LastOpenedForUserDTO.class);
        eventBus.register(this);

        db.createIndex(Indexes.ascending(LastOpenedForUserDTO.FIELD_USER_ID));
        db.createIndex(Indexes.ascending(LastOpenedForUserDTO.FIELD_ITEMS + "." + LastOpenedDTO.FIELD_GRN));
    }

    public Optional<LastOpenedForUserDTO> findForUser(final SearchUser searchUser) {
        return findForUser(searchUser.getUser().getId());
    }

    Optional<LastOpenedForUserDTO> findForUser(final String userId) {
        return MongoUtils.stream(this.db.find(Filters.eq(LastOpenedForUserDTO.FIELD_USER_ID, userId))).findAny();
    }

    public void create(final LastOpenedForUserDTO lastOpenedItems) {
        try {
            db.insertOne(lastOpenedItems);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to create a last opened collection, collection with this id already exists : " + lastOpenedItems.id());
        }
    }

    @Subscribe
    public void removeLastOpenedOnEntityDeletion(final RecentActivityEvent event) {
        // if an entity is deleted, we can no longer see it in the lastOpened collection
        if (event.activityType().equals(ActivityType.DELETE)) {
            final var grn = event.grn().toString();
            final var query = Filters.eq(LastOpenedForUserDTO.FIELD_ITEMS + "." + LastOpenedDTO.FIELD_GRN, grn);
            final var modifications = Updates.pull(LastOpenedForUserDTO.FIELD_ITEMS, Filters.eq(LastOpenedDTO.FIELD_GRN, grn));
            db.updateMany(query, modifications);
        }
    }

    @Subscribe
    public void removeFavoriteEntityOnUserDeletion(final UserDeletedEvent event) {
        db.deleteMany(Filters.eq(LastOpenedForUserDTO.FIELD_USER_ID, event.userId()));
    }

    public void save(LastOpenedForUserDTO loi) {
        if (loi.id() == null) {
            create(loi);
        } else {
            db.replaceOne(MongoUtils.idEq(loi.id()), loi);
        }
    }

    @VisibleForTesting
    long count() {
        return this.db.countDocuments();
    }
}
