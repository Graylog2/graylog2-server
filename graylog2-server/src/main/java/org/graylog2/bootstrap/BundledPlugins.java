package org.graylog2.bootstrap;

import org.graylog.plugins.threatintel.ThreatIntelPlugin;
import org.graylog2.plugin.Plugin;

import java.util.Set;

public class BundledPlugins {
    public static final Set<Plugin> BUNDLED_PLUGINS = Set.of(
            new ThreatIntelPlugin()
    );
}
