package ${package};

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class ${pluginClassName}MetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "${groupId}.${artifactId}/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "${package}.${pluginClassName}Plugin";
    }

    @Override
    public String getName() {
        return "${pluginClassName}";
    }

    @Override
    public String getAuthor() {
        return "${ownerName} <${ownerEmail}>";
    }

    @Override
    public URI getURL() {
        return URI.create("https://github.com/${githubRepo}");
    }

    @Override
    public Version getVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(0, 0, 0, "unknown"));
    }

    @Override
    public String getDescription() {
        // TODO Insert correct plugin description
        return "Description of ${pluginClassName} plugin";
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
