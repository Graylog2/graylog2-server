package org.graylog.plugins.enterprise.search.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;
import org.graylog.plugins.enterprise.search.Filter;

import java.util.Set;

@AutoValue
@JsonTypeName(AndFilter.NAME)
@JsonDeserialize(builder = AutoValue_AndFilter.Builder.class)
public abstract class AndFilter implements Filter {
    public static final String NAME = "and";

    @Override
    @JsonProperty
    public abstract String type();

    @Override
    @JsonProperty
    public abstract Set<Filter> filters();

    public static Builder builder() {
        return Builder.create();
    }

    public static AndFilter and(Filter... filters) {
        return builder()
                .filters(Sets.newHashSet(filters))
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder filters(Set<Filter> filters);

        public abstract AndFilter build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_AndFilter.Builder().type(NAME);
        }
    }
}
