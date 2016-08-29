/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.dashboards.widgets;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.dashboards.widgets.events.WidgetUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class WidgetEventsListener {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetEventsListener.class);

    private final WidgetResultCache widgetResultCache;

    @Inject
    public WidgetEventsListener(WidgetResultCache widgetResultCache,
                                EventBus eventBus) {
        this.widgetResultCache = widgetResultCache;
        eventBus.register(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void invalidateWidgetResultCacheForWidgetUpdated(WidgetUpdatedEvent widgetUpdatedEvent) {
        LOG.debug("Invalidating widget <" + widgetUpdatedEvent.widgetId() + "> from WidgetResultCache due to WidgetUpdatedEvent");
        this.widgetResultCache.invalidate(widgetUpdatedEvent.widgetId());
    }
}
