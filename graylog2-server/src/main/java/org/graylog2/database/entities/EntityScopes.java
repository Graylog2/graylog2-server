package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
public abstract class EntityScopes {

    private static final String FIELD_ENTITY_SCOPES = "entity_scopes";

    @JsonProperty(FIELD_ENTITY_SCOPES)
    public abstract Map<String, EntityScopeResponse> entityScopes();

    @JsonCreator
    public static EntityScopes create(@JsonProperty("entity_scopes") Map<String, EntityScopeResponse> entityScopes) {
        return new AutoValue_EntityScopes(entityScopes);
    }


}
