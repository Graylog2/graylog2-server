package org.graylog2.rest.models.system.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class ConfigurationList {

    @JsonProperty
    public abstract List<ConfigurationVariable> variables();

    @JsonCreator
    public static ConfigurationList create(@JsonProperty("variables") List<ConfigurationVariable> variables) {
        return new AutoValue_ConfigurationList(variables);
    }

}
