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
package org.graylog2.rest.resources.dashboards;

import org.apache.shiro.subject.Subject;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.WidgetResultCache;
import org.graylog2.dashboards.widgets.events.WidgetUpdatedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.models.dashboards.requests.AddWidgetRequest;
import org.graylog2.rest.models.dashboards.requests.UpdateWidgetRequest;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DashboardWidgetsResourceTest {
    private DashboardWidgetsResource dashboardWidgetsResource;
    @Mock
    private DashboardWidgetCreator dashboardWidgetCreator;
    @Mock
    private ActivityWriter activityWriter;
    @Mock
    private WidgetResultCache widgetResultCache;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private Subject subject;

    @Mock
    private DashboardWidget dashboardWidget;
    @Mock
    private Dashboard dashboard;

    private final String dashboardId = "dashboardId";
    private final String widgetId = "widgetId";
    private final String creatorUserId = "mockuser";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.dashboardWidgetsResource = new DashboardWidgetsResource(
            dashboardWidgetCreator,
            activityWriter,
            widgetResultCache,
            dashboardService,
            clusterEventBus
        ) {
            @Override
            protected void checkPermission(String permission) {
            }

            @Override
            protected void checkPermission(String permission, String instanceId) {
            }

            @Override
            protected void checkAnyPermission(String[] permissions, String instanceId) {
            }

            @Override
            protected Subject getSubject() {
                return subject;
            }
        };

        when(dashboardWidget.getCreatorUserId()).thenReturn(creatorUserId);
        when(dashboardWidget.getId()).thenReturn(widgetId);
        when(this.dashboardService.load(eq(dashboardId))).thenReturn(dashboard);
        when(dashboard.getWidget(eq(widgetId))).thenReturn(dashboardWidget);
    }

    @Test
    public void updateWidgetMustSendUpdatedWidgetEvent() throws Exception {
        final DashboardWidget updatedWidget = mock(DashboardWidget.class);
        when(updatedWidget.getId()).thenReturn(widgetId);
        final AddWidgetRequest addWidgetRequest = AddWidgetRequest.create("new description", "new type", 60, Collections.emptyMap());

        when(dashboardWidgetCreator.fromRequest(eq(widgetId), eq(addWidgetRequest), eq(creatorUserId))).thenReturn(updatedWidget);

        this.dashboardWidgetsResource.updateWidget(dashboardId, widgetId, addWidgetRequest);

        verifyWidgetUpdatedEvent(widgetId);
    }

    @Test
    public void updateCacheTime() throws Exception {
        final int newCacheTime = 666;

        final UpdateWidgetRequest updateWidgetRequest = UpdateWidgetRequest.create(null, newCacheTime);
        this.dashboardWidgetsResource.updateCacheTime(this.dashboardId, this.widgetId, updateWidgetRequest);

        verifyWidgetUpdatedEvent(widgetId);
    }

    private void verifyWidgetUpdatedEvent(String widgetId) {
        final ArgumentCaptor<WidgetUpdatedEvent> widgetUpdatedEventCaptor = ArgumentCaptor.forClass(WidgetUpdatedEvent.class);
        verify(clusterEventBus).post(widgetUpdatedEventCaptor.capture());

        final WidgetUpdatedEvent capturedEvent = widgetUpdatedEventCaptor.getValue();

        assert(capturedEvent.widgetId()).equals(widgetId);
    }
}
