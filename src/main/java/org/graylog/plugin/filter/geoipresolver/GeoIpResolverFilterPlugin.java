package org.graylog.plugin.filter.geoipresolver;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Arrays;
import java.util.Collection;

public class GeoIpResolverFilterPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new GeoIpResolverFilterMetaData();
    }

    @Override
    public Collection<PluginModule> modules () {
        return Arrays.<PluginModule>asList(new GeoIpResolverFilterModule());
    }
}
