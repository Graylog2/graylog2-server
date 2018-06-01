package org.graylog.plugins.sidecar.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class EtagCacheInvalidation {

    @JsonProperty("etag")
    public abstract String etag();

    @JsonCreator
    public static EtagCacheInvalidation etag(@JsonProperty("etag") String etag) {
        return new AutoValue_EtagCacheInvalidation(etag);
    }

}
