package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.Collector;

import java.util.Collection;

@AutoValue
public abstract class CollectorListResponse {
    @JsonProperty
    public abstract long total();

    @JsonProperty
    public abstract Collection<Collector> collectors();

    @JsonCreator
    public static CollectorListResponse create(@JsonProperty("total") long total,
                                               @JsonProperty("sidecars") Collection<Collector> collectors) {
        return new AutoValue_CollectorListResponse(total, collectors);
    }
}
