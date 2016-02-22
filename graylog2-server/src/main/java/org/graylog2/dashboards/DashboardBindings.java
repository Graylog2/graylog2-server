package org.graylog2.dashboards;

import com.google.inject.Scopes;
import org.graylog2.dashboards.widgets.WidgetResultCache;
import org.graylog2.plugin.inject.Graylog2Module;

public class DashboardBindings extends Graylog2Module {
    @Override
    protected void configure() {
        bind(WidgetResultCache.class).in(Scopes.SINGLETON);
    }
}
