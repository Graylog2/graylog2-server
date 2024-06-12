package org.graylog2.entitygroups.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.entitygroups.model.EntityGroup;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog2.entitygroups.model.EntityGroup.FIELD_ENTITIES;
import static org.graylog2.entitygroups.model.EntityGroup.FIELD_NAME;

public record EntityGroupRequest(
        @JsonProperty(FIELD_NAME)
        String name,
        @Nullable
        @JsonProperty(FIELD_ENTITIES)
        Map<String, Set<String>> entities
) {
    public EntityGroup toEntityGroup() {
        return EntityGroup.builder()
                .name(name())
                .entities(firstNonNull(entities(), Map.of()))
                .build();
    }
}
