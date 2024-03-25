package org.graylog.plugins.views.search.views.widgets.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Filter(@JsonProperty(FIELD_FIELD) String field, @JsonProperty(FIELD_VALUE) List<String> value) {
    private static final String FIELD_FIELD = "field";
    private static final String FIELD_VALUE = "value";

    @JsonCreator
    public static Filter create(@JsonProperty(FIELD_FIELD) String field, @JsonProperty(FIELD_VALUE) List<String> value) {
        return new Filter(field, value);
    }
}
