package org.graylog.plugins.views.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.engine.BackendQuery;

@AutoValue
@JsonTypeName(ElasticsearchQueryString.NAME)
@JsonDeserialize(builder = ElasticsearchQueryString.Builder.class)
public abstract class ElasticsearchQueryString implements BackendQuery {

    public static final String NAME = "elasticsearch";

    @Override
    public abstract String type();

    @JsonProperty
    public abstract String queryString();

    public static Builder builder() {
        return new AutoValue_ElasticsearchQueryString.Builder().type(NAME);
    }

    @Override
    public String toString() {
        return type() + ": " + queryString();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder queryString(String queryString);

        public abstract ElasticsearchQueryString build();
    }
}
