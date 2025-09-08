package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.entities.source.EntitySource;

import java.util.Optional;

public interface SourcedScopedEntity<B extends SourcedScopedEntity.Builder<B>> extends ScopedEntity<B> {
    String FIELD_ENTITY_SOURCE = "_entity_source";

    /**
     * The entity source information, if available. This field is populated via the
     * {@link org.graylog2.database.pagination.EntitySourceLookup} aggregation stage from the "entity_source" collection
     * and is not stored directly in any entity's collection. The access and inclusion annotations enforce this behavior.
     */
    @JsonProperty(value = FIELD_ENTITY_SOURCE, access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<EntitySource> entitySource();

    interface Builder<B> extends ScopedEntity.Builder<B> {

        @JsonProperty(FIELD_ENTITY_SOURCE)
        B entitySource(Optional<EntitySource> source);
    }
}
