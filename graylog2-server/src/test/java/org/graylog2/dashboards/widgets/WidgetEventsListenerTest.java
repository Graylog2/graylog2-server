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
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class WidgetEventsListenerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WidgetResultCache widgetResultCache;
    private WidgetEventsListener widgetEventsListener;

    @Before
    public void setUp() throws Exception {
        widgetEventsListener = new WidgetEventsListener(widgetResultCache, new EventBus("Test"));
    }

    @Test
    public void invalidateWidgetResultCacheForWidgetUpdatedMustInvalidateWidgetResultCacheForWidgetId() throws Exception {
        final String widgetId = "mockedId";
        final WidgetUpdatedEvent widgetUpdatedEvent = WidgetUpdatedEvent.create(widgetId);

        widgetEventsListener.invalidateWidgetResultCacheForWidgetUpdated(widgetUpdatedEvent);

        final ArgumentCaptor<String> widgetIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(widgetResultCache).invalidate(widgetIdCaptor.capture());
        assertThat(widgetIdCaptor.getValue()).isEqualTo(widgetId);
    }

}
