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
 * A base database service to handle persistence and deletion of {@link ScopedEntity} instance.
 * Persistence and deletion is performed by the parent class, {@link PaginatedDbService},
 * this service simply performs mutability checks.
 *
 * <p>
 * A {@link EntityScopeService} is used to perform the actual mutability checks based on the entity's <b>scope</b>.
 * </p>
 *
 * @param <E> type parameter for a {@link ScopedEntity}'s subclass.
 */
public abstract class ScopedDbService<E extends ScopedEntity> extends PaginatedDbService<E> {

    protected final EntityScopeService entityScopeService;

    public ScopedDbService(MongoConnection mongoConnection,
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
            return entityScopeService.isMutable(current.get(), scopedEntity);
        }

        // The entity does not exist in the database, This could be a new entity--check it
        return entityScopeService.isMutable(scopedEntity);
    }

    public final boolean isDeletable(ScopedEntity scopedEntity) {

        Objects.requireNonNull(scopedEntity, "Entity must not be null");

        // First, check whether this entity has been persisted, if so, the persisted entity's scope takes precedence.
        Optional<E> current = get(scopedEntity.id());
        if (current.isPresent()) {
            return entityScopeService.isDeletable(current.get());
        }

        // The entity does not exist in the database, This could be a new entity--check it
        return entityScopeService.isDeletable(scopedEntity);
    }

    @Override
    public E save(E entity) {

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

    public final void ensureDeletability(E entity) {
        if (!isDeletable(entity)) {
            throw new IllegalArgumentException("Non-deletable entity cannot be deleted");
        }
    }

    @Override
    public final int delete(String id) {
        final E entity = get(id).orElseThrow(() -> new IllegalArgumentException("Entity not found"));
        ensureDeletability(entity);
        ensureMutability(entity);

        return super.delete(id);
    }

    /**
     * Deletes an entity without checking for deletability. Do not call this method for API requests for the user interface.
     */
    public final int forceDelete(String id) {
        // Intentionally omit ensure mutability check.
        return super.delete(id);
    }

    /**
     * Saves an entity without checking for mutability. Do not call this method for API requests for the user interface.
     */
    public final E forceSave(E entity) {
        // Intentionally omit ensure mutability check.
        return super.save(entity);
    }
}
