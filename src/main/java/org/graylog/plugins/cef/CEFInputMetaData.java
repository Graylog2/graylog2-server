package org.graylog.plugins.cef;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class CEFInputMetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "org.graylog.plugins.graylog-plugin-input-cef/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "org.graylog.plugins.cef.CEFInputPlugin";
    }

    @Override
    public String getName() {
        return "CEF Input";
    }

    @Override
    public String getAuthor() {
        return "Lennart Koopmann <lennart@graylog.com>";
    }

    @Override
    public URI getURL() {
        return URI.create("https://github.com/Graylog2/graylog-plugin-cef");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public String getDescription() {
        return "Input plugin to receive CEF (Common Event Format) messages.";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(2, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
