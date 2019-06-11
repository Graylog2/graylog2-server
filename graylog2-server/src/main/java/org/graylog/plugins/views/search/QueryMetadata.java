package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
public abstract class QueryMetadata {

    @JsonProperty("used_parameters_names")
    public abstract ImmutableSet<String> usedParameterNames();

    @JsonProperty("referenced_queries")
    public abstract ImmutableSet<String> referencedQueries();

    public static QueryMetadata empty() {
        return QueryMetadata.builder().build();
    }

    public static Builder builder() {
        return new AutoValue_QueryMetadata.Builder()
                .usedParameterNames(of())
                .referencedQueries(of());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("used_parameters_names")
        public abstract Builder usedParameterNames(Set<String> usedParameterNames);

        @JsonProperty("referenced_queries")
        public abstract Builder referencedQueries(Set<String> referencedQueries);

        public abstract QueryMetadata build();
    }
}
