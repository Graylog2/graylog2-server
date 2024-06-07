package org.graylog2.entitygroups.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BulkEntityGroupRequest(
        @JsonProperty(FIELD_ENTITY_TYPE)
        String type,
        @JsonProperty(FIELD_ENTITY_IDS)
        List<String> entityIds
) {
    public static final String FIELD_ENTITY_TYPE = "entity_type";
    public static final String FIELD_ENTITY_IDS = "entity_IDS";
}
