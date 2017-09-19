package org.graylog.plugins.enterprise.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.BackendQuery;

@AutoValue
@JsonTypeName(ElasticsearchQuery.TYPE)
public abstract class ElasticsearchQuery implements BackendQuery {

    public static final String TYPE = "elasticsearch";

    @Override
    public abstract String type();

    public abstract String queryString();

    public static Builder builder() {
        return new AutoValue_ElasticsearchQuery.Builder().type(TYPE);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String type);

        public abstract Builder queryString(String queryString);

        public abstract ElasticsearchQuery build();
    }
}
