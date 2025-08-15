package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.MongoEntity;

import java.util.Optional;

public interface SourcedMongoEntity extends MongoEntity {
    String FIELD_ENTITY_SOURCE = "entity_source";

    @JsonProperty(value = FIELD_ENTITY_SOURCE, access = JsonProperty.Access.READ_ONLY)
    Optional<EntitySource> entitySource();
}
