package org.graylog.storage.elasticsearch6;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class Elasticsearch6Metadata implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return Elasticsearch6Plugin.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "Elasticsearch 6 Support";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org");
    }

    @Override
    public Version getVersion() {
        return Version.CURRENT_CLASSPATH;
    }

    @Override
    public String getDescription() {
        return "Support for Elasticsearch 6";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.CURRENT_CLASSPATH;
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
