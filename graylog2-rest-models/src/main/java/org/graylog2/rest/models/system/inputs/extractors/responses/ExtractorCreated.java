package org.graylog2.rest.models.system.inputs.extractors.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class ExtractorCreated {
    @JsonProperty("extractor_id")
    public abstract String extractorId();

    @JsonCreator
    public static ExtractorCreated create(@JsonProperty("extractor_id") String extractorId) {
        return new AutoValue_ExtractorCreated(extractorId);
    }
}
