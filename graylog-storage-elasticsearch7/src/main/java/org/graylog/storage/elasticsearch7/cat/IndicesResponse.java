package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class IndicesResponse {
    @JsonCreator
    public static IndicesResponse create() {
        return new AutoValue_IndicesResponse();
    }
}
