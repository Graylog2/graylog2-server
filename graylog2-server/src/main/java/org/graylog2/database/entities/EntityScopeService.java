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


import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityScopeService {

    private final Map<String, EntityScope> entityScopes;

    @Inject
    public EntityScopeService(Set<EntityScope> entityScopes) {
        this.entityScopes = Objects.requireNonNull(entityScopes)
                .stream()
                .collect(Collectors.toMap(EntityScope::getName, e -> e));
    }

    public List<EntityScope> getEntityScopes() {

        return Collections.unmodifiableList(new ArrayList<>(entityScopes.values()));
    }

    public boolean isMutable(ScopedEntity scopedEntity) {

        Objects.requireNonNull(scopedEntity, "Entity must not be null");

        String scope = scopedEntity.scope();

        // TODO: Should it fail instead?
        if (scope == null || scope.isEmpty()) {
            return true;
        }

        EntityScope entityScope = entityScopes.get(scope);
        if (entityScope == null) {
            throw new IllegalArgumentException("Entity Scope does not exist: " + scope);
        }

        return entityScope.isMutable();

    }

    // TODO: Give further consideration to whether the scope can be null here.
    public boolean hasValidScope(ScopedEntity scopedEntity) {
        Objects.requireNonNull(scopedEntity, "Entity must not be null");

        String scope = scopedEntity.scope();

        return scope == null || entityScopes.containsKey(scope);
    }
}
