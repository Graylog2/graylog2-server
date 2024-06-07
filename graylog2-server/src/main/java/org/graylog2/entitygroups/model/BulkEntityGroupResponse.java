package org.graylog2.entitygroups.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Map;

public record BulkEntityGroupResponse(@JsonProperty(EntityGroup.FIELD_ENTITIES)
                                      Map<String, Collection<EntityGroup>> entityGroups) {
}
