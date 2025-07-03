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


import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class EntityScopeService {

    private final Map<String, EntityScope> entityScopes;

    @Inject
    public EntityScopeService(Set<EntityScope> entityScopes) {
        this.entityScopes = Objects.requireNonNull(entityScopes)
                .stream()
                .collect(Collectors.toMap(e -> e.getName().toUpperCase(Locale.ROOT), e -> e));
    }

    public List<EntityScope> getEntityScopes() {
        return List.copyOf(entityScopes.values());
    }

    public boolean isMutable(ScopedEntity scopedEntity) {
        Objects.requireNonNull(scopedEntity, "Entity must not be null");
        String scopeName = scopedEntity.scope();

        if (scopeName == null || scopeName.isEmpty()) {
            return true;
        }
        final EntityScope scope = entityScopes.get(scopeName.toUpperCase(Locale.ROOT));
        if (scope == null) {
            throw new IllegalArgumentException("Entity Scope does not exist: " + scopeName);
        }
        return scope.isMutable(scopedEntity);
    }

    public boolean isMutable(ScopedEntity existingEntity, ScopedEntity updatedEntity) {
        Objects.requireNonNull(existingEntity, "Entity must not be null");
        String scope = existingEntity.scope();
        if (scope == null || scope.isEmpty()) {
            return true;
        }

        EntityScope entityScope = entityScopes.get(scope.toUpperCase(Locale.ROOT));
        if (entityScope == null) {
            throw new IllegalArgumentException("Entity Scope does not exist: " + scope);
        }

        return entityScope.isMutable(existingEntity, updatedEntity);
    }

    public boolean isDeletable(ScopedEntity scopedEntity) {
        Objects.requireNonNull(scopedEntity, "Entity must not be null");
        String scopeName = scopedEntity.scope();

        if (scopeName == null || scopeName.isEmpty()) {
            return true;
        }
        final EntityScope scope = entityScopes.get(scopeName.toUpperCase(Locale.ROOT));
        if (scope == null) {
            throw new IllegalArgumentException("Entity Scope does not exist: " + scopeName);
        }
        return scope.isDeletable(scopedEntity);
    }

    public boolean hasValidScope(ScopedEntity scopedEntity) {
        Objects.requireNonNull(scopedEntity, "Entity must not be null");
        String scope = scopedEntity.scope();

        return scope != null && entityScopes.containsKey(scope);
    }
}
