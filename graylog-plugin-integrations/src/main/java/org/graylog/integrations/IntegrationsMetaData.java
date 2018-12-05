package org.graylog.integrations;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class IntegrationsMetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "org.graylog.integrations.graylog-plugin-enterprise-integrations/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "org.graylog.integrations.IntegrationsPlugin";
    }

    @Override
    public String getName() {
        return "Integrations";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc. <hello@graylog.com>";
    }

    @Override
    public URI getURL() {
        return URI.create("https://github.com/git@github.com:Graylog2/graylog-plugin-enterprise-integrations.git");
    }

    @Override
    public Version getVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(0, 0, 0, "unknown"));
    }

    @Override
    public String getDescription() {
        // TODO Insert correct plugin description
        return "Description of Integrations plugin";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "graylog.version", Version.from(0, 0, 0, "unknown"));
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
