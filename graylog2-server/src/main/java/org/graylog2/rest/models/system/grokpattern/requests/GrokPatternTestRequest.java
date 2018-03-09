package org.graylog2.rest.models.system.grokpattern.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.grok.GrokPattern;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class GrokPatternTestRequest {

    @JsonProperty
    public abstract GrokPattern grokPattern();

    @JsonProperty
    public abstract String sampleData();

    @JsonCreator
    public static GrokPatternTestRequest create(@JsonProperty("grok_pattern") GrokPattern grokPattern,
                                         @JsonProperty("sample_data") String sampleData) {
       return new AutoValue_GrokPatternTestRequest(grokPattern, sampleData);
    }
}
