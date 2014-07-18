package org.graylog2.rest.resources.streams.outputs;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.plugin.configuration.ConfigurationRequest;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AvailableOutputSummary {
    public String name;
    public String type;
    @JsonProperty("human_name")
    public String humanName;
    @JsonProperty("link_to_docs")
    public String linkToDocs;
    public ConfigurationRequest requestedConfiguration;
}
