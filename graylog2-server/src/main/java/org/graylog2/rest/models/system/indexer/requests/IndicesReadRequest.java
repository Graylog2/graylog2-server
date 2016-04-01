package org.graylog2.rest.models.system.indexer.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class IndicesReadRequest {
    @JsonProperty("indices")
    public abstract List<String> indices();

    @JsonCreator
    public static IndicesReadRequest create(@JsonProperty("indices") List<String> indices) {
        return new AutoValue_IndicesReadRequest(indices);
    }
}
