package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.graph.Traverser;
import org.graylog.plugins.enterprise.search.engine.BackendQuery;
import org.graylog.plugins.enterprise.search.engine.EmptyTimeRange;
import org.graylog.plugins.enterprise.search.filter.StreamFilter;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableSortedSet.of;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = Query.Builder.class)
public abstract class Query {
    private static final Logger LOG = LoggerFactory.getLogger(Query.class);

    /**
     * Implicitly created by {@link Builder#build} to make looking up search types easier and quicker. Simply a unique index by ID.
     */
    @JsonIgnore
    private ImmutableMap<String, SearchType> searchTypesIndex;

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

    @Nonnull
    @JsonProperty("search_types")
    public abstract ImmutableSet<SearchType> searchTypes();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Query.Builder()
                .searchTypes(of());
    }

    public Query applyExecutionState(ObjectMapper objectMapper, JsonNode state) {
        if (state.isMissingNode()) {
            return this;
        }
        final boolean hasTimerange = state.hasNonNull("timerange");
        final boolean hasSearchTypes = state.hasNonNull("search_types");
        if (hasTimerange || hasSearchTypes) {
            final Builder builder = toBuilder();
            if (hasTimerange) {
                try {
                    final Object rawTimerange = state.path("timerange");
                    final TimeRange newTimeRange = objectMapper.convertValue(rawTimerange, TimeRange.class);
                    builder.timerange(newTimeRange);
                } catch (Exception e) {
                    LOG.error("Unable to deserialize execution state for time range", e);
                }
            }
            if (hasSearchTypes) {
                // copy all existing search types, we'll update them by id if necessary below
                Map<String, SearchType> updatedSearchTypes = Maps.newHashMap(searchTypesIndex);

                state.path("search_types").fields().forEachRemaining(stateEntry -> {
                    final String id = stateEntry.getKey();
                    final SearchType searchType = searchTypesIndex.get(id);
                    final SearchType updatedSearchType = searchType.applyExecutionContext(objectMapper, stateEntry.getValue());
                    updatedSearchTypes.put(id, updatedSearchType);
                });
                builder.searchTypes(ImmutableSet.copyOf(updatedSearchTypes.values()));
            }
            return builder.build();
        }
        return this;
    }

    public static Query emptyRoot() {
        return Query.builder()
                .id("")
                .timerange(EmptyTimeRange.emptyTimeRange())
                .query(new BackendQuery.Fallback())
                .filter(null)
                .build();
    }

    public Set<String> usedStreamIds() {
        if (filter() != null) {
            final Traverser<Filter> filterTraverser = Traverser.forTree(filter -> firstNonNull(filter.filters(), Collections.emptySet()));
            return StreamSupport.stream(filterTraverser.breadthFirst(filter()).spliterator(), false)
                    .filter(filter -> filter instanceof StreamFilter)
                    .map(streamFilter -> ((StreamFilter) streamFilter).streamId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder timerange(TimeRange timerange);

        @JsonProperty
        public abstract Builder filter(Filter filter);

        @JsonProperty
        public abstract Builder query(BackendQuery query);

        @JsonProperty("search_types")
        public abstract Builder searchTypes(@Nullable Set<SearchType> searchTypes);

        abstract Query autoBuild();

        @JsonCreator
        public static Builder createWithDefaults() {
            return Query.builder();
        }

        public Query build() {
            final Query query = autoBuild();
            query.searchTypesIndex = Maps.uniqueIndex(query.searchTypes(), SearchType::id);
            return query;
        }
    }
}
