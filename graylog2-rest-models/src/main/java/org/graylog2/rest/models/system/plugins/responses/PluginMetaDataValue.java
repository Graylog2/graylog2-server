package org.graylog2.rest.models.system.plugins.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.net.URI;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class PluginMetaDataValue {
    @JsonProperty("unique_id")
    public abstract String uniqueId();
    @JsonProperty
    public abstract String name();
    @JsonProperty
    public abstract String author();
    @JsonProperty
    public abstract URI url();
    @JsonProperty
    public abstract String version();
    @JsonProperty
    public abstract String description();
    @JsonProperty("required_version")
    public abstract String requiredVersion();
    @JsonProperty("required_capabilities")
    public abstract Set<String> requiredCapabilities();

    @JsonCreator
    public static PluginMetaDataValue create(@JsonProperty("unique_id") String uniqueId,
                                             @JsonProperty("name") String name,
                                             @JsonProperty("author") String author,
                                             @JsonProperty("url") URI url,
                                             @JsonProperty("version") String version,
                                             @JsonProperty("description") String description,
                                             @JsonProperty("required_version") String requiredVersion,
                                             @JsonProperty("required_capabilities") Set<String> requiredCapabilities) {
        return new AutoValue_PluginMetaDataValue(uniqueId, name, author, url, version, description, requiredVersion, requiredCapabilities);
    }
}
