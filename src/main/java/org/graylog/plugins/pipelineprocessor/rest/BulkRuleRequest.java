package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class BulkRuleRequest {
    @JsonProperty
    public abstract List<String> rules();

    @JsonCreator
    public static BulkRuleRequest create(@JsonProperty("rules") List<String> rules) {
        return new AutoValue_BulkRuleRequest(rules);
    }
}
