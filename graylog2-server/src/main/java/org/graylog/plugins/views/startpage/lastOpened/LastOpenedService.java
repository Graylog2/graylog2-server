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
public class LastOpenedService extends PaginatedDbService<LastOpenedItemsDTO> {
    private static final String COLLECTION_NAME = "last_opened_items";

    private final EntityOwnershipService entityOwnerShipService;

    @Inject
    protected LastOpenedService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper,
                                final EntityOwnershipService entityOwnerShipService) {
        super(mongoConnection, mapper, LastOpenedItemsDTO.class, COLLECTION_NAME);
        this.entityOwnerShipService = entityOwnerShipService;
    }

    public Optional<LastOpenedItemsDTO> findForUser(final SearchUser searchUser) {
        return streamQuery(DBQuery.is(LastOpenedItemsDTO.FIELD_USER_ID, searchUser.getUser().getId())).findAny();
    }

    public Optional<LastOpenedItemsDTO> create(final LastOpenedItemsDTO lastOpenedItems, final SearchUser searchUser) {
        try {
            final WriteResult<LastOpenedItemsDTO, ObjectId> result = db.insert(lastOpenedItems);
            final LastOpenedItemsDTO savedObject = result.getSavedObject();
            if (savedObject != null) {
                entityOwnerShipService.registerNewEntity(savedObject.id(), searchUser.getUser(), GRNTypes.LAST_OPENED_ITEMS);
            }
            return Optional.ofNullable(savedObject);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to create a last opened collection, collection with this id already exists : " + lastOpenedItems.id());
        }
    }
}
