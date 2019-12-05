package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SearchEntity.Builder.class)
public abstract class SearchEntity {
    public static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_OWNER = "owner";

    @JsonProperty
    public abstract ImmutableSet<Query> queries();

    @JsonProperty
    public abstract ImmutableSet<Parameter> parameters();

    @JsonProperty(FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create().parameters(of()).queries(ImmutableSet.<Query>builder().build());
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder queries(ImmutableSet<Query> queries);

        @JsonProperty
        public abstract Builder parameters(ImmutableSet<Parameter> parameters);

        @JsonProperty(FIELD_REQUIRES)
        public abstract Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        public abstract SearchEntity build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SearchEntity.Builder()
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC))
                    .parameters(of());
        }

    }
}
