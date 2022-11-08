package org.graylog2.rest.resources.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Sorting(@JsonProperty("id") String id, @JsonProperty("direction") Direction direction) {
    public enum Direction {
        ASC,
        DESC
    }
    public static Sorting create(String id, Direction direction) {
        return new Sorting(id, direction);
    }
}
