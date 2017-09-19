package org.graylog.plugins.enterprise.search.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.Filter;

import javax.annotation.Nullable;

@AutoValue
@JsonTypeName(StreamFilter.NAME)
@JsonDeserialize(builder = StreamFilter.Builder.class)
public abstract class StreamFilter implements Filter {
    public static final String NAME = "stream";

    @Override
    public abstract String type();

    @Nullable
    @JsonProperty("id")
    public abstract String streamId();

    @Nullable
    @JsonProperty("title")
    public abstract String streamTitle();

    public static Builder builder() {
        return new AutoValue_StreamFilter.Builder().type(NAME);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String type);

        @JsonProperty("id")
        public abstract Builder streamId(@Nullable String streamId);

        @JsonProperty("title")
        public abstract Builder streamTitle(@Nullable String streamTitle);

        public abstract StreamFilter build();
    }
}
