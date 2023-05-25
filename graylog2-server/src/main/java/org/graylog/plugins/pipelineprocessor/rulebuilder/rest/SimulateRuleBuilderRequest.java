package org.graylog.plugins.pipelineprocessor.rulebuilder.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SimulateRuleBuilderRequest(@JsonProperty("message") String message, @JsonProperty("ruleBuilderDto") RuleBuilderDto ruleBuilderDto) {
}
