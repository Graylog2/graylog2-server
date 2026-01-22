package org.graylog.events.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Slice(@JsonProperty(FIELD_ID) String id, @JsonProperty(FIELD_TITLE) String title, @JsonProperty(FIELD_COUNT) Integer count, @JsonProperty(FIELD_TYPE) String type) {
    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_COUNT = "count";
    private static final String FIELD_TYPE = "type";
}
