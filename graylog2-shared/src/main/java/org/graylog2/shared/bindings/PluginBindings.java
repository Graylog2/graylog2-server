package org.graylog2.shared.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;

import java.util.Set;

/**
 * Created by dennis on 15/12/14.
 */
public class PluginBindings extends AbstractModule {
    private final Set<Plugin> plugins;

    public PluginBindings(Set<Plugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    protected void configure() {
        Multibinder<Plugin> pluginbinder = Multibinder.newSetBinder(binder(), Plugin.class);
        Multibinder<PluginMetaData> pluginMetaDataBinder = Multibinder.newSetBinder(binder(), PluginMetaData.class);
        for (Plugin plugin : plugins) {
            pluginbinder.addBinding().toInstance(plugin);
            pluginMetaDataBinder.addBinding().toInstance(plugin.metadata());
        }
    }
}
