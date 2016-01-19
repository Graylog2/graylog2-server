package org.graylog.plugins.pipelineprocessor;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class PipelineProcessorMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return "org.graylog.plugins.pipelineprocessor.PipelineProcessorPlugin";
    }

    @Override
    public String getName() {
        return "PipelineProcessor";
    }

    @Override
    public String getAuthor() {
        // TODO Insert author name
        return "PipelineProcessor author";
    }

    @Override
    public URI getURL() {
        // TODO Insert correct plugin website
        return URI.create("https://www.graylog.org/");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public String getDescription() {
        // TODO Insert correct plugin description
        return "Description of PipelineProcessor plugin";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(1, 2, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
