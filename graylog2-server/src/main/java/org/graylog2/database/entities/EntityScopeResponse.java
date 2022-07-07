package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Objects;

@AutoValue
public abstract class EntityScopeResponse {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_IS_MUTABLE = "is_mutable";

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_IS_MUTABLE)
    public abstract boolean mutable();

    @JsonCreator
    public static EntityScopeResponse create(@JsonProperty(FIELD_NAME) String name,
                                             @JsonProperty(FIELD_IS_MUTABLE) boolean mutable) {
        return new AutoValue_EntityScopeResponse(name, mutable);
    }

    public static EntityScopeResponse of(EntityScope scope) {

        Objects.requireNonNull(scope, "Entity Scope must not be null");

        return create(scope.getName(), scope.isMutable());
    }
}
