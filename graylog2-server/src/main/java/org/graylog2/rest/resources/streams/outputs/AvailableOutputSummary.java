package org.graylog2.rest.resources.streams.outputs;

import org.graylog2.plugin.configuration.ConfigurationRequest;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AvailableOutputSummary {
    public String name;
    public ConfigurationRequest requestedConfiguration;
}
