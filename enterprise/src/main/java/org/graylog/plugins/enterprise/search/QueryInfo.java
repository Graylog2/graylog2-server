package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

@AutoValue
public abstract class QueryInfo {

    @JsonProperty
    public abstract ImmutableMap<String, QueryParameter> parameters();

    public static QueryInfo empty() {
        return QueryInfo.builder().parameters(ImmutableMap.of()).build();
    }

    public static Builder builder() {
        return new AutoValue_QueryInfo.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder parameters(@Nullable ImmutableMap<String, QueryParameter> parameters);

        public abstract QueryInfo build();
    }
}
