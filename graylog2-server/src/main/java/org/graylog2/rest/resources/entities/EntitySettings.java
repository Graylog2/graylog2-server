package org.graylog2.rest.resources.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class EntitySettings {
    @JsonProperty("attributes")
    public abstract List<String> attributes();

    @JsonProperty("sort")
    public abstract Sorting sort();

    public static Builder builder() {
        return Builder.builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder attributes(List<String> attributes);
        public abstract Builder sort(Sorting sort);

        public static Builder builder() {
            return new AutoValue_EntitySettings.Builder();
        }
        public abstract EntitySettings build();
    }
}
