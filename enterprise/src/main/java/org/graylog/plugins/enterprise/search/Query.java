package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.graylog.plugins.enterprise.search.engine.BackendQuery;
import org.graylog.plugins.enterprise.search.engine.EmptyTimeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

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

    @Nonnull
    @JsonProperty
    public abstract ImmutableSet<Parameter> parameters();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Query.Builder()
                .searchTypes(of())
                .parameters(of());
    }

    public Query applyExecutionState(ObjectMapper objectMapper, Map<String, Object> state) {
        if (state == null) {
            return this;
        }
        final boolean hasTimerange = state.containsKey("timerange");
        final boolean hasSearchTypes = state.containsKey("search_types");
        if (hasTimerange || hasSearchTypes) {
            final Builder builder = toBuilder();
            if (hasTimerange) {
                try {
                    final Object rawTimerange = state.get("timerange");
                    final TimeRange newTimeRange = objectMapper.treeToValue(objectMapper.valueToTree(rawTimerange), TimeRange.class);
                    builder.timerange(newTimeRange);
                } catch (JsonProcessingException e) {
                    LOG.error("Unable to deserialize execution state for time range", e);
                }
            }
            if (hasSearchTypes) {
                // copy all existing search types, we'll update them by id if necessary below
                Map<String, SearchType> updatedSearchTypes = Maps.newHashMap(searchTypesIndex);

                @SuppressWarnings("unchecked")
                Map<String, Object> searchTypeStates = (Map<String, Object>) state.get("search_types");
                for (Map.Entry<String, Object> stateEntry : searchTypeStates.entrySet()) {
                    final String id = stateEntry.getKey();
                    final SearchType searchType = searchTypesIndex.get(id);
                    @SuppressWarnings("unchecked")
                    final SearchType updatedSearchType = searchType.applyExecutionContext(objectMapper, (Map<String, Object>) stateEntry.getValue());
                    updatedSearchTypes.put(id, updatedSearchType);
                }
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

        @JsonProperty
        public abstract Builder parameters(ImmutableSet<Parameter> parameters);

        abstract Query autoBuild();

        @JsonCreator
        public static Builder createWithDefaults() {
            return Query.builder().parameters(ImmutableSet.of());
        }

        public Query build() {
            final Query query = autoBuild();
            query.searchTypesIndex = Maps.uniqueIndex(query.searchTypes(), SearchType::id);
            return query;
        }
    }
}
