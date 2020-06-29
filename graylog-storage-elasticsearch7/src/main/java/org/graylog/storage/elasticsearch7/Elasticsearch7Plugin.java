package org.graylog.storage.elasticsearch7;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;
import java.util.Collections;

public class Elasticsearch7Plugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new Elasticsearch7Metadata();
    }

    @Override
    public Collection<PluginModule> modules() {
        return Collections.emptySet();
    }
}
