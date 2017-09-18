package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.mongojack.ObjectId;

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

}
