package org.graylog2.dashboards.widgets;

import com.google.common.eventbus.Subscribe;
import org.graylog2.dashboards.widgets.events.WidgetUpdatedEvent;
import org.graylog2.events.ClusterEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class WidgetEventsListener {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetEventsListener.class);

    private final WidgetResultCache widgetResultCache;

    @Inject
    public WidgetEventsListener(WidgetResultCache widgetResultCache,
                                ClusterEventBus clusterEventBus) {
        this.widgetResultCache = widgetResultCache;
        clusterEventBus.register(this);
    }

    @Subscribe
    public void invalidateWidgetResultCacheForWidgetUpdated(WidgetUpdatedEvent widgetUpdatedEvent) {
        LOG.debug("Invalidating widget <" + widgetUpdatedEvent.widgetId() + "> from WidgetResultCache due to WidgetUpdatedEvent");
        this.widgetResultCache.invalidate(widgetUpdatedEvent.widgetId());
    }
}
