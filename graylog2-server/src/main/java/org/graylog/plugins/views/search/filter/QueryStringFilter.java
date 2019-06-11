package org.graylog.plugins.views.search.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonTypeName(QueryStringFilter.NAME)
@JsonDeserialize(builder = QueryStringFilter.Builder.class)
public abstract class QueryStringFilter implements Filter {

    public static final String NAME = "query_string";

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    public abstract Set<Filter> filters();

    @JsonProperty("query")
    public abstract String query();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_QueryStringFilter.Builder().type(NAME);
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder filters(@Nullable Set<Filter> filters);

        @JsonProperty
        public abstract Builder query(String query);

        public abstract QueryStringFilter build();
    }
}

