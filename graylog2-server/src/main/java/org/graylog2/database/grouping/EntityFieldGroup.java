package org.graylog2.database.grouping;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EntityFieldGroup(@JsonProperty("id") String fieldValue,
                               @JsonProperty("value") String fieldTitle,
                               @JsonProperty("count") long count) {
}
