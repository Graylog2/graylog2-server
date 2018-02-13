package org.graylog.plugins.cef;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;
import java.util.Collections;

public class CEFInputPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new CEFInputMetaData();
    }

    @Override
    public Collection<PluginModule> modules() {
        return Collections.singleton(new CEFInputModule());
    }
}
