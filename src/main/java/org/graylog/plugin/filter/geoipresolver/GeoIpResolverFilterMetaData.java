package org.graylog.plugin.filter.geoipresolver;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class GeoIpResolverFilterMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return "org.graylog.plugin.filter.geoipresolver.GeoIpResolverFilterPlugin";
    }

    @Override
    public String getName() {
        return "GeoIpResolverFilter";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org/");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public String getDescription() {
        return "Resolves GeoIp information from messages";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
