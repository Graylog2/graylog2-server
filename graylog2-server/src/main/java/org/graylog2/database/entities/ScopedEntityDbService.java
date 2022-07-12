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
package org.graylog2.database.entities;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;

/**
 * A base database service to handle persistence and deletion of {@link ScopedEntity} instance.  Persistence and deletion is performed by the parent class, {@link PaginatedDbService},  this service simply performs mutability checks.
 *
 * <p>
 * A {@link EntityScopeService} is used to perform the actual mutability checks based on the entity's <b>scope</b>.
 * </p>
 *
 * @param <E> type parameter for a {@link ScopedEntity}'s subclass.
 */
public abstract class ScopedEntityDbService<E extends ScopedEntity> extends PaginatedDbService<E> {

    protected final EntityScopeService entityScopeService;

    public ScopedEntityDbService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper,
                                 Class<E> dtoClass, String collectionName,
                                 EntityScopeService entityScopeService) {
        super(mongoConnection, mapper, dtoClass, collectionName);
        this.entityScopeService = entityScopeService;
    }

    @Override
    public E save(E entity) {

        ensureValidScope(entity);
        if (entity.id() != null) {
            ensureMutability(entity);
        }
        return super.save(entity);
    }

    private void ensureValidScope(E entity) {
        if (!entityScopeService.hasValidScope(entity)) {
            throw new IllegalArgumentException("Invalid Entity Scope: " + entity.scope());
        }
    }


    private void ensureMutability(E entity) {
        if (!entityScopeService.isMutable(entity)) {
            throw new IllegalArgumentException("Immutable entity cannot be modified");
        }
    }

    @Override
    public int delete(String id) {
        ensureMutability(get(id).orElseThrow(() -> new IllegalArgumentException("Entity not found")));

        return super.delete(id);
    }
}
