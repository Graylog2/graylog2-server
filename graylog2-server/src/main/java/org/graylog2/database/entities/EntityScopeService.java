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

    public boolean isMutable(Entity entity) {

        Objects.requireNonNull(entity, "Entity must not be null");
        Objects.requireNonNull(entity.metadata(), "Entity Metadata must not be null");
        String scope = entity.metadata().scope();

        if (scope == null || scope.isEmpty()) {
            throw new IllegalArgumentException("Entity Scope must not be empty");
        }

        EntityScope entityScope = entityScopes.get(scope);
        if (entityScope == null) {
            throw new IllegalArgumentException("Entity Scope does not exist: " + scope);
        }

        return entityScope.isMutable();

    }

    public boolean hasValidScope(Entity entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        Objects.requireNonNull(entity.metadata(), "Entity Metadata must not be null");
        String scope = entity.metadata().scope();

        return entityScopes.containsKey(scope);
    }
}
