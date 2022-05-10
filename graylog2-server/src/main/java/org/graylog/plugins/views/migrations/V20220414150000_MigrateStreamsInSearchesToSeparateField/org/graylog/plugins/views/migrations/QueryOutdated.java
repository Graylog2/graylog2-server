package org.graylog.plugins.views.migrations.V20220414150000_MigrateStreamsInSearchesToSeparateField.org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.GlobalOverride;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.ImmutableSortedSet.of;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = QueryOutdated.Builder.class)
public abstract class QueryOutdated {

    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty
    public abstract Filter filter();

    @Nonnull
    @JsonProperty
    public abstract BackendQuery query();

    @JsonIgnore
    public abstract Optional<GlobalOverride> globalOverride();

    @Nonnull
    @JsonProperty("search_types")
    public abstract ImmutableSet<SearchType> searchTypes();

    public abstract QueryOutdated.Builder toBuilder();

    public Query migrateToNewQueryFormat(final Set<String> streams) {
        return Query.builder()
                .id(this.id())
                .timerange(this.timerange())
                .filter(null)
                .streams(streams)
                .query(this.query())
                .globalOverride(this.globalOverride().orElse(null))
                .searchTypes(this.searchTypes())
                .build();
    }

    public Query migrateWithNoChange() {
        return Query.builder()
                .id(this.id())
                .timerange(this.timerange())
                .filter(this.filter())
                .streams(Collections.emptySet())
                .query(this.query())
                .globalOverride(this.globalOverride().orElse(null))
                .searchTypes(this.searchTypes())
                .build();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        @JsonProperty
        public abstract QueryOutdated.Builder id(String id);

        public abstract String id();

        @JsonProperty
        public abstract QueryOutdated.Builder timerange(TimeRange timerange);

        @JsonProperty
        public abstract QueryOutdated.Builder filter(Filter filter);

        @JsonProperty
        public abstract QueryOutdated.Builder query(BackendQuery query);

        public abstract QueryOutdated.Builder globalOverride(@Nullable GlobalOverride globalOverride);

        @JsonProperty("search_types")
        public abstract QueryOutdated.Builder searchTypes(@Nullable Set<SearchType> searchTypes);

        abstract QueryOutdated autoBuild();

        @JsonCreator
        static QueryOutdated.Builder createWithDefaults() {
            try {
                return new AutoValue_QueryOutdated.Builder()
                        .searchTypes(of())
                        .query(ElasticsearchQueryString.empty())
                        .timerange(RelativeRange.create(300));
            } catch (InvalidRangeParametersException e) {
                throw new RuntimeException("Unable to create relative timerange - this should not happen!");
            }
        }

        public QueryOutdated build() {
            if (id() == null) {
                id(UUID.randomUUID().toString());
            }
            return autoBuild();
        }
    }
}
