package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class SearchMetadata {

    @JsonProperty
    public abstract Map<String, QueryMetadata> queryMetadata();

    @JsonProperty("declared_parameters")
    public abstract ImmutableSet<Parameter> declaredParameters();

    public static SearchMetadata create(Map<String, QueryMetadata> queryMetadata, ImmutableSet<Parameter> declaredParameters) {
        return new AutoValue_SearchMetadata(queryMetadata, declaredParameters);
    }

}
