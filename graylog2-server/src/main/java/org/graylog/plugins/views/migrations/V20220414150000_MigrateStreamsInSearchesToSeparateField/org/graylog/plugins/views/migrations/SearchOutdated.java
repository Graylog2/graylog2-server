package org.graylog.plugins.views.migrations.V20220414150000_MigrateStreamsInSearchesToSeparateField.org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SearchOutdated.Builder.class)
public abstract class SearchOutdated {

    public static final String FIELD_REQUIRES = "requires";
    static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_OWNER = "owner";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract ImmutableSet<QueryOutdated> queries();

    @JsonProperty
    public abstract ImmutableSet<Parameter> parameters();

    @JsonProperty(FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

    public SearchOutdated withOwner(@Nonnull String owner) {
        return toBuilder().owner(owner).build();
    }

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public abstract SearchOutdated.Builder toBuilder();

    public Search migrateToNewSearchFormat(final ImmutableSet<Query> newQueries) {
        return Search.builder()
                .id(this.id())
                .parameters(this.parameters())
                .requires(this.requires())
                .owner(this.owner().orElse(null))
                .createdAt(this.createdAt())
                .queries(newQueries)
                .build();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        @Id
        @JsonProperty
        public abstract SearchOutdated.Builder id(String id);

        public abstract String id();

        @JsonProperty
        public abstract SearchOutdated.Builder queries(ImmutableSet<QueryOutdated> queries);

        @JsonProperty
        public abstract SearchOutdated.Builder parameters(ImmutableSet<Parameter> parameters);

        @JsonProperty(FIELD_REQUIRES)
        public abstract SearchOutdated.Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(FIELD_OWNER)
        public abstract SearchOutdated.Builder owner(@Nullable String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract SearchOutdated.Builder createdAt(DateTime createdAt);

        abstract SearchOutdated autoBuild();

        @JsonCreator
        public static SearchOutdated.Builder create() {
            return new AutoValue_SearchOutdated.Builder()
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC))
                    .parameters(of());
        }

        public SearchOutdated build() {
            if (id() == null) {
                id(org.bson.types.ObjectId.get().toString());
            }

            return autoBuild();
        }
    }

}
