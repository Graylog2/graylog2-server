package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class SearchDecorationStats {
    private static final String FIELD_ADDED_FIELDS = "added_fields";

    @SuppressWarnings("unused")
    @JsonProperty(FIELD_ADDED_FIELDS)
    public abstract Set<String> addedFields();

    @JsonCreator
    public static SearchDecorationStats create(@JsonProperty(FIELD_ADDED_FIELDS) Set<String> addedFields) {
        return new AutoValue_SearchDecorationStats(addedFields);
    }
}
