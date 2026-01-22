package org.graylog.events.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = EventsSlicesRequest.Builder.class)
public abstract class EventsSlicesRequest {
    private static final String FIELD_PARAMETERS = "parameters";
    private static final String FIELD_SLICE_COLUMN = "slice_column";
    private static final String FIELD_SORT_BY = "sort_by";
    private static final String FIELD_SORT_DIRECTION = "sort_direction";
    private static final String FIELD_INCLUDE_ALL = "include_all";

    public enum SortDirection {
        @JsonProperty("asc")
        ASC,
        @JsonProperty("desc")
        DESC
    }

    @JsonProperty(FIELD_PARAMETERS)
    public abstract EventsSearchParameters parameters();

    @JsonProperty(FIELD_SLICE_COLUMN)
    public abstract String sliceColumn();

    @JsonProperty(FIELD_SORT_BY)
    public abstract String sortBy();

    @JsonProperty(FIELD_SORT_DIRECTION)
    public abstract SortDirection sortDirection();

    @JsonProperty(FIELD_INCLUDE_ALL)
    public abstract boolean includeAll();

    public static Builder builder() {
        return Builder.create();
    }

    public static EventsSlicesRequest empty() {
        return builder().build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventsSlicesRequest.Builder()
                    .includeAll(false)
                    .sortBy("title")
                    .sortDirection(SortDirection.ASC);
        }

        @JsonProperty(FIELD_PARAMETERS)
        public abstract Builder parameters(EventsSearchParameters parameters);

        @JsonProperty(FIELD_SLICE_COLUMN)
        public abstract Builder sliceColumn(String sliceColumn);

        @JsonProperty(FIELD_SORT_BY)
        public abstract Builder sortBy(String sortBy);

        @JsonProperty(FIELD_SORT_DIRECTION)
        public abstract Builder sortDirection(SortDirection sortDirection);

        @JsonProperty(FIELD_INCLUDE_ALL)
        public abstract Builder includeAll(boolean includeAll);

        public abstract EventsSlicesRequest build();
    }
}
