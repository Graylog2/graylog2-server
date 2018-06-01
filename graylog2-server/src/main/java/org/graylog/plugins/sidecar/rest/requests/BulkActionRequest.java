package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class BulkActionRequest {
    @JsonProperty
    public abstract String sidecarId();

    @JsonProperty
    public abstract List<String> collectorIds();

    @JsonCreator
    public static BulkActionRequest create(@JsonProperty("sidecar_id") String collectorId,
                                           @JsonProperty("collector_ids") List<String> collectorIds) {
        return new AutoValue_BulkActionRequest(collectorId, collectorIds);
    }
}