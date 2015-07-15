package org.graylog.plugins.netflow;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Arrays;
import java.util.Collection;

/**
 * Implement the Plugin interface here.
 */
public class NetflowPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new NetflowPluginMetaData();
    }

    @Override
    public Collection<PluginModule> modules () {
        return Arrays.<PluginModule>asList(new NetflowPluginModule());
    }
}
