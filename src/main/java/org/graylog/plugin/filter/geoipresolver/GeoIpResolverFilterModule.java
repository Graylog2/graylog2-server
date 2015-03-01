package org.graylog.plugin.filter.geoipresolver;

import com.google.common.collect.Sets;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Set;

public class GeoIpResolverFilterModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Sets.newHashSet(new GeoIpResolverConfiguration());
    }

    @Override
    protected void configure() {
        addMessageFilter(GeoIpResolverFilter.class);
        addConfigBeans();
    }
}
