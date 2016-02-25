package org.graylog.plugins.map;

import org.graylog.plugins.map.geoip.filter.GeoIpResolverFilter;
import org.graylog.plugins.map.rest.MapDataResource;
import org.graylog.plugins.map.widget.strategy.MapWidgetStrategy;
import org.graylog2.plugin.PluginModule;

public class MapWidgetModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageFilter(GeoIpResolverFilter.class);
        addWidgetStrategy(MapWidgetStrategy.class, MapWidgetStrategy.Factory.class);
        addRestResource(MapDataResource.class);
    }
}
