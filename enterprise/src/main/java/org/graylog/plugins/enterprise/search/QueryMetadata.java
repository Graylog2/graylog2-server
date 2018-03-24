package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

import static com.google.common.collect.ImmutableMap.of;

@AutoValue
public abstract class QueryMetadata {

    @JsonProperty
    public abstract ImmutableMap<String, Parameter> parameters();

    @JsonProperty
    public abstract ImmutableMap<String, Parameter> unusedParameters();

    public static QueryMetadata empty() {
        return QueryMetadata.builder().build();
    }

    public static Builder builder() {
        return new AutoValue_QueryMetadata.Builder()
                .parameters(of())
                .unusedParameters(of());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder parameters(@Nullable ImmutableMap<String, Parameter> parameters);

        public abstract QueryMetadata build();

        public abstract Builder unusedParameters(ImmutableMap<String, Parameter> unusedParams);
    }
}
