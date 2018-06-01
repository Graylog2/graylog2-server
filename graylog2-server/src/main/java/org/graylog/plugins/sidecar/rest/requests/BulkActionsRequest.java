package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class BulkActionsRequest {
    @JsonProperty
    public abstract String action();

    @JsonProperty
    public abstract List<BulkActionRequest> collectors();

    @JsonCreator
    public static BulkActionsRequest create(@JsonProperty("action") String action,
                                            @JsonProperty("collectors") List<BulkActionRequest> collectors) {
        return new AutoValue_BulkActionsRequest(action, collectors);
    }
}