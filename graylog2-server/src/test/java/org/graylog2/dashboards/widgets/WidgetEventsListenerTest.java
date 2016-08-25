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
import org.graylog2.dashboards.widgets.events.WidgetUpdatedEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WidgetEventsListenerTest {
    @Mock
    private WidgetResultCache widgetResultCache;
    @Mock
    private EventBus eventBus;

    private WidgetEventsListener widgetEventsListener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.widgetEventsListener = new WidgetEventsListener(this.widgetResultCache, eventBus);
        verify(eventBus).register(any());
    }

    @Test
    public void invalidateWidgetResultCacheForWidgetUpdatedMustInvalidateWidgetResultCacheForWidgetId() throws Exception {
        final String widgetId = "mockedId";
        final WidgetUpdatedEvent widgetUpdatedEvent = mock(WidgetUpdatedEvent.class);
        when(widgetUpdatedEvent.widgetId()).thenReturn(widgetId);

        this.widgetEventsListener.invalidateWidgetResultCacheForWidgetUpdated(widgetUpdatedEvent);

        final ArgumentCaptor<String> widgetIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.widgetResultCache).invalidate(widgetIdCaptor.capture());

        assert(widgetIdCaptor.getValue()).equals(widgetId);
    }

}
