package org.graylog.storage.elasticsearch6;

import com.google.common.collect.ImmutableList;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;

public class Elasticsearch6Plugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new Elasticsearch6Metadata();
    }

    @Override
    public Collection<PluginModule> modules() {
        return ImmutableList.of(
                new Elasticsearch6Module(),
                new ViewsESBackendModule()
        );
    }
}
