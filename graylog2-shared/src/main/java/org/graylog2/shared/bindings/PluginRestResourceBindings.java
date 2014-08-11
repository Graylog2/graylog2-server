package org.graylog2.shared.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.rest.PluginRestResource;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PluginRestResourceBindings extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<String, PluginRestResource> pluginRestResourceMapBinder = MapBinder.newMapBinder(binder(), String.class, PluginRestResource.class).permitDuplicates();
    }
}
