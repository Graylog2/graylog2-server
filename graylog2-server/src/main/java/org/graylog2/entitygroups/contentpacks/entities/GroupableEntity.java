package org.graylog2.entitygroups.contentpacks.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

public interface GroupableEntity {
    String FIELD_ENTITY_GROUPS = "entity_groups";

    @Nullable
    @JsonProperty(FIELD_ENTITY_GROUPS)
    List<String> entityGroups();

    @JsonIgnore
    String groupedEntityTypeName();
}
