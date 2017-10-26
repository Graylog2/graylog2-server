package org.graylog.plugins.enterprise.search;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog.plugins.enterprise.search.engine.BackendQuery;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.mongojack.Id;
import org.mongojack.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = AutoValue_Query.Builder.class)
public abstract class Query {
    private static final Logger LOG = LoggerFactory.getLogger(Query.class);

    /**
     * Implicitely created by {@link Builder#build} to make looking up search types easier and quicker. Simply a unique index by ID.
     */
    @JsonIgnore
    private ImmutableMap<String, SearchType> searchTypesIndex;

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty
    public abstract Filter filter();

    @Nullable
    @JsonProperty
    public abstract BackendQuery query();

    @Nullable
    @JsonProperty("search_types")
    public abstract ImmutableList<SearchType> searchTypes();

    @Nullable
    @JsonProperty
    public abstract Map<String, ParameterBinding> parameters();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Query.Builder();
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

                Map<String, Object> searchTypeStates = (Map<String, Object>) state.get("search_types");
                for (Map.Entry<String, Object> stateEntry : searchTypeStates.entrySet()) {
                    final String id = stateEntry.getKey();
                    final SearchType searchType = searchTypesIndex.get(id);
                    final SearchType updatedSearchType = searchType.applyExecutionContext(objectMapper, (Map<String, Object>) stateEntry.getValue());
                    updatedSearchTypes.put(id, updatedSearchType);
                }
                builder.searchTypes(ImmutableList.copyOf(updatedSearchTypes.values()));
            }
            return builder.build();
        }
        return this;
    }

    /**
     * Search queries require
     *
     * @return a Query instance with IDs assigned to each of its search types.
     */
    public Query withSearchTypeIds() {
        final List<SearchType> searchTypes = searchTypes();
        if (searchTypes != null) {
            return this.toBuilder().searchTypes(searchTypes.stream()
                    .map(searchType -> {
                        if (searchType.id() == null) {
                            return searchType.withId(new UUID().toString());
                        } else {
                            return searchType;
                        }
                    })
                    .collect(Collectors.toList())).build();
        }
        return this;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @Id
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder timerange(TimeRange timerange);

        @JsonProperty
        public abstract Builder filter(Filter filter);

        @JsonProperty
        public abstract Builder query(BackendQuery query);

        @JsonProperty("search_types")
        public abstract Builder searchTypes(@Nullable List<SearchType> searchTypes);

        @JsonProperty
        public abstract Builder parameters(Map<String, ParameterBinding> parameters);

        abstract Query autoBuild();

        public Query build() {
            final Query query = autoBuild();
            final ImmutableList<SearchType> searchTypes = query.searchTypes();
            if (searchTypes != null) {
                query.searchTypesIndex = Maps.uniqueIndex(searchTypes, SearchType::id);
            }
            return query;
        }
    }
}
