package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
public abstract class QueryMetadata {

    @JsonProperty("used_parameters_names")
    public abstract ImmutableSet<String> usedParameterNames();

    public static QueryMetadata empty() {
        return QueryMetadata.builder().build();
    }

    public static Builder builder() {
        return new AutoValue_QueryMetadata.Builder()
                .usedParameterNames(of());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("used_parameters_names")
        public abstract Builder usedParameterNames(Set<String> usedParameterNames);

        public abstract QueryMetadata build();
    }
}
