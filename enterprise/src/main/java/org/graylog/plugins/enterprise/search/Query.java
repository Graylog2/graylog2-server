package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class Query {

    @Nullable
    @JsonProperty
    public abstract String id();

    @Nullable
    @JsonProperty
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty
    public abstract List<Filter> filters();

    @Nullable
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

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timerange(TimeRange timerange);

        public abstract Builder filters(List<Filter> filters);

        public abstract Builder query(BackendQuery query);

        public abstract Builder searchTypes(List<SearchType> searchTypes);

        public abstract Builder parameters(Map<String, ParameterBinding> parameters);

        public abstract Builder queries(List<Query> queries);

        public abstract Query build();
    }
}
