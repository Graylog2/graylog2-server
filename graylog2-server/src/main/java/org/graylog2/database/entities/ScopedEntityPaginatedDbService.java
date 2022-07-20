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

import java.util.Objects;
import java.util.Optional;

/**
 * A base database service to handle persistence and deletion of {@link ScopedEntity} instance.  Persistence and deletion is performed by the parent class, {@link PaginatedDbService},  this service simply performs mutability checks.
 *
 * <p>
 * A {@link EntityScopeService} is used to perform the actual mutability checks based on the entity's <b>scope</b>.
 * </p>
 *
 * @param <E> type parameter for a {@link ScopedEntity}'s subclass.
 */
public abstract class ScopedEntityPaginatedDbService<E extends ScopedEntity> extends PaginatedDbService<E> {

    protected final EntityScopeService entityScopeService;

    public ScopedEntityPaginatedDbService(MongoConnection mongoConnection,
                                          MongoJackObjectMapperProvider mapper,
                                          Class<E> dtoClass, String collectionName,
                                          EntityScopeService entityScopeService) {
        super(mongoConnection, mapper, dtoClass, collectionName);
        this.entityScopeService = entityScopeService;
    }

    public final boolean isMutable(ScopedEntity scopedEntity) {

        Objects.requireNonNull(scopedEntity, "Entity must not be null");

        // First, check whether this entity has been persisted, if so, the persisted entity's scope takes precedence.
        Optional<E> current = get(scopedEntity.id());
        if (current.isPresent()) {
            return entityScopeService.isMutable(current.get());
        }

        // The entity does not exist in the database, This could be a new entity--check it
        return entityScopeService.isMutable(scopedEntity);
    }

    @Override
    public final E save(E entity) {

        ensureValidScope(entity);
        if (entity.id() != null) {
            ensureMutability(entity);
        }
        return super.save(entity);
    }

    public final void ensureValidScope(E entity) {
        if (!entityScopeService.hasValidScope(entity)) {
            throw new IllegalArgumentException("Invalid Entity Scope: " + entity.scope());
        }
    }


    public final void ensureMutability(E entity) {
        if (!isMutable(entity)) {
            throw new IllegalArgumentException("Immutable entity cannot be modified");
        }
    }

    @Override
    public final int delete(String id) {
        ensureMutability(get(id).orElseThrow(() -> new IllegalArgumentException("Entity not found")));

        return super.delete(id);
    }

    /**
     * Deletes a mutable entity. Do not call this method for API requests which support the user interface.
     * Only call when mutable deletion is appropriate (for example when deleting from content packs service, which
     * is an appropriate deletion override path for Illuminate).
     */
    public final int deleteMutable(String id) {
        // Intentionally omit ensure mutability check.
        return super.delete(id);
    }
}
