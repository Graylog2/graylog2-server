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
package org.graylog.plugins.views.favorites;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.startpage.recentActivities.ActivityType;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityEvent;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.lookup.Catalog;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.users.events.UserDeletedEvent;
import org.mongojack.DBQuery;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FavoritesService extends PaginatedDbService<FavoritesForUserDTO> {
    public static final String COLLECTION_NAME = "favorites";

    private final EntityOwnershipService entityOwnerShipService;
    private final Catalog catalog;
    private final GRNRegistry grnRegistry;

    @Inject
    protected FavoritesService(final MongoConnection mongoConnection,
                               EventBus eventBus,
                               final MongoJackObjectMapperProvider mapper,
                               final EntityOwnershipService entityOwnerShipService,
                               final Catalog catalog,
                               final GRNRegistry grnRegistry) {
        super(mongoConnection, mapper, FavoritesForUserDTO.class, COLLECTION_NAME);
        eventBus.register(this);
        this.entityOwnerShipService = entityOwnerShipService;
        this.catalog = catalog;
        this.grnRegistry = grnRegistry;

        db.createIndex(new BasicDBObject(FavoritesForUserDTO.FIELD_USER_ID, 1));
        db.createIndex(new BasicDBObject(FavoritesForUserDTO.FIELD_ITEMS, 1));
    }

    public PaginatedResponse<Favorite> findFavoritesFor(final SearchUser searchUser, final Optional<String> type, final int page, final int perPage) {
        var items = this.findForUser(searchUser)
                .orElse(new FavoritesForUserDTO(searchUser.getUser().getId(), List.of()))
                .items()
                .stream().filter(i -> type.isPresent() ? i.type().equals(type.get()) : true)
                .map(i -> new Favorite(i, catalog.getTitle(i)))
                .toList();

        return PaginatedResponse.create("favorites", new PaginatedList<>(getPage(items, page, perPage), items.size(), page, perPage));
    }

    public void addFavoriteItemFor(final String in, final SearchUser searchUser) {
        var grn = grnRegistry.parse(in);
        final var favorites = this.findForUser(searchUser);
        if(favorites.isPresent()) {
            var fi = favorites.get();
            fi.items().add(grn);
            this.save(fi);
        } else {
            var items = new FavoritesForUserDTO(searchUser.getUser().getId(), List.of(grn));
            this.create(items, searchUser);
        }
    }

    public void removeFavoriteItemFor(final String in, final SearchUser searchUser) {
        var grn = grnRegistry.parse(in);
        var favorites = this.findForUser(searchUser);
        if(favorites.isPresent() && favorites.get().items() != null) {
            var fi = favorites.get();
            var items = fi.items().stream().filter(i -> !i.equals(grn)).toList();
            fi.items().clear();
            fi.items().addAll(items);
            this.save(fi);
        }
    }

    Optional<FavoritesForUserDTO> findForUser(final SearchUser searchUser) {
        return findForUser(searchUser.getUser().getId());
    }

    Optional<FavoritesForUserDTO> findForUser(final String userId) {
        return streamQuery(DBQuery.is(FavoritesForUserDTO.FIELD_USER_ID, userId)).findAny();
    }

    public Optional<FavoritesForUserDTO> create(final FavoritesForUserDTO favorite, final SearchUser searchUser) {
        try {
            final WriteResult<FavoritesForUserDTO, ObjectId> result = db.insert(favorite);
            final FavoritesForUserDTO savedObject = result.getSavedObject();
            if (savedObject != null) {
                entityOwnerShipService.registerNewEntity(savedObject.id(), searchUser.getUser(), GRNTypes.FAVORITE);
            }
            return Optional.ofNullable(savedObject);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to create a Favorites collection, collection with this id already exists : " + favorite.id());
        }
    }

    @Subscribe
    public void removeFavoriteOnEntityDeletion(final RecentActivityEvent event) {
        // if an entity is deleted, we can no longer see it in the favorites collection
        if (event.activityType().equals(ActivityType.DELETE)) {
            final var grn = event.grn().toString();
            final var query = new BasicDBObject(FavoritesForUserDTO.FIELD_ITEMS, grn);
            final var modifications = new BasicDBObject("$pull", new BasicDBObject(FavoritesForUserDTO.FIELD_ITEMS, grn));
            db.updateMulti(query, modifications);
        }
    }

    @Subscribe
    public void removeFavoriteEntityOnUserDeletion(final UserDeletedEvent event) {
        db.remove(DBQuery.is(FavoritesForUserDTO.FIELD_USER_ID, event.userId()));
    }
}
