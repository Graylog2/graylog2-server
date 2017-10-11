package org.graylog.plugins.enterprise.search;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.engine.BackendQuery;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = AutoValue_Query.Builder.class)
public abstract class Query {

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @Nullable
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
    public abstract List<SearchType> searchTypes();

    @Nullable
    @JsonProperty
    public abstract Map<String, ParameterBinding> parameters();

    @Nullable
    @JsonProperty
    public abstract List<Query> queries();

    public static Builder builder() {
        return new AutoValue_Query.Builder();
    }

    public abstract Builder toBuilder();

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

        @JsonProperty
        public abstract Builder searchTypes(List<SearchType> searchTypes);

        @JsonProperty
        public abstract Builder parameters(Map<String, ParameterBinding> parameters);

        @JsonProperty
        public abstract Builder queries(List<Query> queries);

        public abstract Query build();
    }
}
