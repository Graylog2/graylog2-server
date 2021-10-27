package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.ValidationRequest;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = ValidationRequestDTO.Builder.class)
public abstract class ValidationRequestDTO {

    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_TIMERANGE = "timerange";

    @JsonProperty
    public abstract BackendQuery query();

    @Nullable
    @JsonProperty(FIELD_TIMERANGE)
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty
        public abstract ValidationRequestDTO.Builder query(BackendQuery query);


        @JsonProperty(FIELD_STREAMS)
        public abstract ValidationRequestDTO.Builder streams(@Nullable Set<String> streams);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract ValidationRequestDTO.Builder timerange(@Nullable TimeRange timerange);

        public abstract ValidationRequestDTO build();

        @JsonCreator
        public static ValidationRequestDTO.Builder builder() {
            return new AutoValue_ValidationRequestDTO.Builder();
        }
    }
}
