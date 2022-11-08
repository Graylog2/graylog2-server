package org.graylog2.rest.resources.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoValue
public abstract class EntityAttribute {
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("type")
    @Nullable
    public abstract String type();

    @JsonProperty("sortable")
    @Nullable
    public abstract Boolean sortable();

    @JsonProperty("filterable")
    @Nullable
    public abstract Boolean filterable();

    @JsonProperty("filter_options")
    @Nullable
    public abstract Set<FilterOption> filterOptions();

    public static Builder builder() {
        return Builder.builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder id(String id);
        public abstract Builder title(String title);
        public abstract Builder type(String type);
        public abstract Builder sortable(Boolean sortable);
        public abstract Builder filterable(Boolean filterable);
        public abstract Builder filterOptions(Set<FilterOption> filterOptions);
        public abstract EntityAttribute build();

        public static Builder builder() {
            return new AutoValue_EntityAttribute.Builder()
                    .sortable(true);
        }
    }
}
