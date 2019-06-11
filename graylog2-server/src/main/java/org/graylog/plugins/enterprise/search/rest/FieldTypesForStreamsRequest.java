package org.graylog.plugins.enterprise.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = FieldTypesForStreamsRequest.Builder.class)
public abstract class FieldTypesForStreamsRequest {
    private static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        public abstract FieldTypesForStreamsRequest build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_FieldTypesForStreamsRequest.Builder();
        }
    }
}
