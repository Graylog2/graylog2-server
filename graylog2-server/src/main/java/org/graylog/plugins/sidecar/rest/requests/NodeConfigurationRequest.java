package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.NodeConfiguration;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class NodeConfigurationRequest {
    @JsonProperty
    public abstract List<NodeConfiguration> nodes();

    @JsonCreator
    public static NodeConfigurationRequest create(
            @JsonProperty("nodes") List<NodeConfiguration> nodeConfigurations) {
        return new AutoValue_NodeConfigurationRequest(nodeConfigurations);
    }
}
