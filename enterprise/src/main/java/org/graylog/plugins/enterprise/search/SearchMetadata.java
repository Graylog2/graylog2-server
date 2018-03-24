package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class SearchMetadata {

    @JsonProperty
    public abstract Map<String, QueryMetadata> queryMetadata();

    public static SearchMetadata create(Map<String, QueryMetadata> queryMetadata) {
        return new AutoValue_SearchMetadata(queryMetadata);
    }

}
