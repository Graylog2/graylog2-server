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
package org.graylog2.database.utils;

import com.mongodb.client.MongoCollection;
import org.bson.types.ObjectId;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ScopedEntity;

import java.util.Objects;
import java.util.Optional;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

public class ScopedEntityMongoUtils<T extends ScopedEntity> {
    private final MongoCollection<T> collection;
    private final EntityScopeService entityScopeService;

    public ScopedEntityMongoUtils(MongoCollection<T> delegate,
                                  EntityScopeService entityScopeService) {
        this.collection = delegate;
        this.entityScopeService = entityScopeService;
    }

    /**
     * Performs a valid scope and mutability check before updating an existing entity.
     *
     * @param entity ScopedEntity to be updated
     * @return the newly updated entity
     */
    public T update(T entity) {
        Objects.requireNonNull(entity.id());
        ensureValidScope(entity);
        ensureMutability(entity);
        collection.replaceOne(idEq(Objects.requireNonNull(entity.id())), entity);
        return entity;
    }

    /**
     * Performs a valid scope check before inserting the entity into the DB.
     *
     * @param entity ScopedEntity to be created
     * @return the ID of the newly created ScopedEntity
     */
    public String create(T entity) {
        ensureValidScope(entity);
        return insertedIdAsString(collection.insertOne(entity));
    }

    /**
     * Convenience method to delete a single document identified by its ID after performing mutability checks.
     *
     * @param id Hex string representation of the document's {@link ObjectId}.
     * @return true if a document was deleted, false otherwise.
     */
    public boolean deleteById(String id) {
        return deleteById(new ObjectId(id));
    }

    /**
     * Convenience method to delete a single document identified by its ID after performing mutability checks.
     *
     * @param id the document's id.
     * @return true if a document was deleted, false otherwise.
     */
    public boolean deleteById(ObjectId id) {
        final T entity = Optional.ofNullable(collection.find(idEq(id)).first())
                .orElseThrow(() -> new IllegalArgumentException("Entity not found"));
        ensureDeletability(entity);
        ensureMutability(entity);
        return collection.deleteOne(idEq(id)).getDeletedCount() > 0;
    }

    /**
     * Deletes an entity without checking for deletability. Do not call this method for API requests for the user
     * interface.
     *
     * @param id ID of the ScopedEntity to be deleted
     */
    public final long forceDelete(String id) {
        // Intentionally omit ensure mutability check.
        return collection.deleteOne(idEq(id)).getDeletedCount();
    }

    public final boolean isMutable(T scopedEntity) {
        Objects.requireNonNull(scopedEntity, "Entity must not be null");

        // First, check whether this entity has been persisted, if so, the persisted entity's scope takes precedence.
        // Else, the entity does not exist in the database, This could be a new entity--check it
        Optional<T> current = scopedEntity.id() == null ? Optional.empty()
                : Optional.ofNullable(collection.find(idEq(scopedEntity.id())).first());
        return current
                .map(t -> entityScopeService.isMutable(t, scopedEntity))
                .orElseGet(() -> entityScopeService.isMutable(scopedEntity));
    }

    public final boolean isDeletable(T scopedEntity) {
        Objects.requireNonNull(scopedEntity, "Entity must not be null");

        // First, check whether this entity has been persisted, if so, the persisted entity's scope takes precedence.
        // Else, the entity does not exist in the database, This could be a new entity--check it
        Optional<T> current = scopedEntity.id() == null ? Optional.empty()
                : Optional.ofNullable(collection.find(idEq(scopedEntity.id())).first());
        return current
                .map(entityScopeService::isDeletable)
                .orElseGet(() -> entityScopeService.isDeletable(scopedEntity));
    }

    public final void ensureValidScope(T entity) {
        if (!entityScopeService.hasValidScope(entity)) {
            throw new IllegalArgumentException("Invalid Entity Scope: " + entity.scope());
        }
    }

    public final void ensureMutability(T entity) {
        if (!isMutable(entity)) {
            throw new IllegalArgumentException("Immutable entity cannot be modified");
        }
    }

    public final void ensureDeletability(T entity) {
        if (!isDeletable(entity)) {
            throw new IllegalArgumentException("Non-deletable entity cannot be deleted");
        }
    }
}
