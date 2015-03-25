package org.graylog2.rest.models.system.inputs.extractors.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.metrics.responses.TimerRateMetricsResponse;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class ExtractorMetrics {

    @JsonProperty
    public abstract TimerRateMetricsResponse total();

    @JsonProperty
    public abstract TimerRateMetricsResponse converters();

    @JsonCreator
    public static ExtractorMetrics create(@JsonProperty("total") TimerRateMetricsResponse total,
                                          @JsonProperty("converters") TimerRateMetricsResponse converters) {
        return new AutoValue_ExtractorMetrics(total, converters);
    }
}
