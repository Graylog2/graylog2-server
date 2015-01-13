package org.graylog2.rest.models.radio.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class PersistedInputsSummaryResponse {
    @JsonProperty
    public abstract int total();
    @JsonProperty
    public abstract List<PersistedInputsResponse> inputs();

    @JsonCreator
    public static PersistedInputsSummaryResponse create(@JsonProperty("total") int total, @JsonProperty("inputs") List<PersistedInputsResponse> inputs) {
        return new AutoValue_PersistedInputsSummaryResponse(total, inputs);
    }

    public static PersistedInputsSummaryResponse create(List<PersistedInputsResponse> inputs) {
        return new AutoValue_PersistedInputsSummaryResponse(inputs.size(), inputs);
    }
}
