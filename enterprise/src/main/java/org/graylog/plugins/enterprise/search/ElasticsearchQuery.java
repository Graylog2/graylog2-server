package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName("elasticsearch")
public abstract class ElasticsearchQuery implements BackendQuery {

    @Override
    public abstract String type();

    public abstract String queryString();
}
