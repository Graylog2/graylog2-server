package org.graylog2.rest.models.system.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class ConfigurationVariable {

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract Object value();

    @JsonCreator
    public static ConfigurationVariable create(@JsonProperty("name") String name, @JsonProperty("value") String x) {
        return new AutoValue_ConfigurationVariable(name, x);
    }

    @JsonCreator
    public static ConfigurationVariable create(@JsonProperty("name") String name, @JsonProperty("value") Number x) {
        return new AutoValue_ConfigurationVariable(name, x);
    }

    @JsonCreator
    public static ConfigurationVariable create(@JsonProperty("name") String name, @JsonProperty("value") Boolean x) {
        return new AutoValue_ConfigurationVariable(name, x);
    }

}
