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

import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.lookup.Catalog;
import org.graylog2.rest.models.PaginatedResponse;
import org.mongojack.DBQuery;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/*
 * TODO: remove entity, if a user is deleted?
 */
public class FavoritesService extends PaginatedDbService<FavoritesForUserDTO> {
    public static final String COLLECTION_NAME = "favorites";

    private final EntityOwnershipService entityOwnerShipService;
    private final Catalog catalog;

    @Inject
    protected FavoritesService(final MongoConnection mongoConnection,
                               final MongoJackObjectMapperProvider mapper,
                               final EntityOwnershipService entityOwnerShipService,
                               final Catalog catalog) {
        super(mongoConnection, mapper, FavoritesForUserDTO.class, COLLECTION_NAME);
        this.entityOwnerShipService = entityOwnerShipService;
        this.catalog = catalog;
    }

    public PaginatedResponse<Favorite> findFavoritesFor(final SearchUser searchUser, final Optional<String> type, final int page, final int perPage) {
        var items = this.findForUser(searchUser)
                .orElse(new FavoritesForUserDTO(searchUser.getUser().getId(), List.of()))
                .items()
                .stream().filter(i -> type.isPresent() ? i.type().equals(type.get()) : true)
                .map(i -> new Favorite(i.id(), i.type(), catalog.getTitle(i.id())))
                .toList();

        return PaginatedResponse.create("favorites", new PaginatedList<>(getPage(items, page, perPage), items.size(), page, perPage));
    }

    public void addFavoriteItemFor(final String id, final SearchUser searchUser) {
        final var favorites = this.findForUser(searchUser);
        final var item = new FavoriteDTO(id, catalog.getType(id));
        if(favorites.isPresent()) {
            var fi = favorites.get();
            fi.items().add(item);
            this.save(fi);
        } else {
            var items = new FavoritesForUserDTO(searchUser.getUser().getId(), List.of(item));
            this.create(items, searchUser);
        }
    }

    public void removeFavoriteItemFor(final String id, final SearchUser searchUser) {
        var favorites = this.findForUser(searchUser);
        if(favorites.isPresent() && favorites.get().items() != null) {
            var fi = favorites.get();
            var items = fi.items().stream().filter(i -> !i.id().equals(id)).toList();
            fi.items().clear();
            fi.items().addAll(items);
            this.save(fi);
        }
    }

    protected Optional<FavoritesForUserDTO> findForUser(final SearchUser searchUser) {
        return streamQuery(DBQuery.is(FavoritesForUserDTO.FIELD_USER_ID, searchUser.getUser().getId())).findAny();
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
}
