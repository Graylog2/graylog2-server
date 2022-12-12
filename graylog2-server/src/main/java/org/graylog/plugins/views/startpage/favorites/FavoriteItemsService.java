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
package org.graylog.plugins.views.startpage.favorites;

import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.mongojack.DBQuery;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Optional;

/*
 * TODO: remove entity, if a user is deleted?
 */
public class FavoriteItemsService extends PaginatedDbService<FavoriteItemsPerUserDTO> {
    private static final String COLLECTION_NAME = "favorite_items";

    private final EntityOwnershipService entityOwnerShipService;

    @Inject
    protected FavoriteItemsService(final MongoConnection mongoConnection,
                                   final MongoJackObjectMapperProvider mapper,
                                   final EntityOwnershipService entityOwnerShipService) {
        super(mongoConnection, mapper, FavoriteItemsPerUserDTO.class, COLLECTION_NAME);
        this.entityOwnerShipService = entityOwnerShipService;
    }

    public Optional<FavoriteItemsPerUserDTO> findForUser(final SearchUser searchUser) {
        return streamQuery(DBQuery.is(FavoriteItemsPerUserDTO.FIELD_USER_ID, searchUser.getUser().getId())).findAny();
    }

    public Optional<FavoriteItemsPerUserDTO> create(final FavoriteItemsPerUserDTO favoriteItem, final SearchUser searchUser) {
        try {
            final WriteResult<FavoriteItemsPerUserDTO, ObjectId> result = db.insert(favoriteItem);
            final FavoriteItemsPerUserDTO savedObject = result.getSavedObject();
            if (savedObject != null) {
                entityOwnerShipService.registerNewEntity(savedObject.id(), searchUser.getUser(), GRNTypes.FAVORITE_ITEMS);
            }
            return Optional.ofNullable(savedObject);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to create a Favorite Items collection, collection with this id already exists : " + favoriteItem.id());
        }
    }
}
