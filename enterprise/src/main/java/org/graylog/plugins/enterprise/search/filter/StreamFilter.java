package org.graylog.plugins.enterprise.search.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.Filter;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonTypeName(StreamFilter.NAME)
@JsonDeserialize(builder = StreamFilter.Builder.class)
public abstract class StreamFilter implements Filter {
    public static final String NAME = "stream";

    @Override
    public abstract String type();

    @Override
    @Nullable
    public abstract Set<Filter> filters();

    @Nullable
    @JsonProperty("id")
    public abstract String streamId();

    @Nullable
    @JsonProperty("title")
    public abstract String streamTitle();

    public static Builder builder() {
        return new AutoValue_StreamFilter.Builder().type(NAME);
    }

    public static StreamFilter ofId(String id) {
        return builder().streamId(id).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String type);

        public abstract Builder filters(@Nullable Set<Filter> filters);

        @JsonProperty("id")
        public abstract Builder streamId(@Nullable String streamId);

        @JsonProperty("title")
        public abstract Builder streamTitle(@Nullable String streamTitle);

        public abstract StreamFilter build();
    }
}
