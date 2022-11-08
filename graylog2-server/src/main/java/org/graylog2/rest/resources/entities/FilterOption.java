package org.graylog2.rest.resources.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FilterOption(@JsonProperty("id") String id, @JsonProperty("title") String title) {
    public static FilterOption create(String id, String title) {
        return new FilterOption(id, title);
    }
}
