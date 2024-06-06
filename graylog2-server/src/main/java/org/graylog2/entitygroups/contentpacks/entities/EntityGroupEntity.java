package org.graylog2.entitygroups.contentpacks.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.ScopedContentPackEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.entitygroups.model.EntityType;

import java.util.List;
import java.util.Map;

import static org.graylog2.entitygroups.model.EntityGroup.FIELD_ENTITIES;

@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = EntityGroupEntity.Builder.class)
public abstract class EntityGroupEntity extends ScopedContentPackEntity implements NativeEntityConverter<EntityGroup> {
    @JsonProperty(FIELD_ENTITIES)
    public abstract Map<EntityType, List<String>> entities();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder extends ScopedContentPackEntity.AbstractBuilder<Builder> {
        @JsonProperty(FIELD_ENTITIES)
        public abstract Builder entities(Map<EntityType, List<String>> entities);

        public abstract EntityGroupEntity build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntityGroupEntity.Builder();
        }
    }

    @Override
    public EntityGroup toNativeEntity(Map<String, ValueReference> parameters,
                                               Map<EntityDescriptor, Object> nativeEntities) {
        // TODO: Need to convert content pack IDs to DB object IDs for the entities map here.
        return null;
    }

    @Override
    public void resolveForInstallation(EntityV1 entity,
                                       Map<String, ValueReference> parameters,
                                       Map<EntityDescriptor, Entity> entities,
                                       MutableGraph<Entity> graph) {
        // TODO: Need to flag all entity dependencies in the entities map so that they get installed first.
        // TODO: They will need to exist in the DB with IDs we can reference once this `toNativeEntity` is called.
    }
}
