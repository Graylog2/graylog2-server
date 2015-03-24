package org.graylog2.rest.models.system.inputs.extractors.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class ExtractorMetrics {

    @JsonProperty
    public abstract Map<String, Object> total();

    @JsonProperty
    public abstract Map<String, Object> converters();

    @JsonCreator
    public static ExtractorMetrics create(@JsonProperty("total") Map<String, Object> total,
                                          @JsonProperty("converters") Map<String, Object> converters) {
        return new AutoValue_ExtractorMetrics(total, converters);
    }
}
