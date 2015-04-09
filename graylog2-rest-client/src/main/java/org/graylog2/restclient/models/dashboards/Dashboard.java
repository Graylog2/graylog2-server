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
package org.graylog2.restclient.models.dashboards;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.graylog2.restclient.models.api.requests.dashboards.AddWidgetRequest;
import org.graylog2.restclient.models.api.requests.dashboards.UpdateDashboardRequest;
import org.graylog2.restclient.models.api.requests.dashboards.UserSetWidgetPositionsRequest;
import org.graylog2.restclient.models.api.requests.dashboards.UserWidgetPositionRequest;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardSummaryResponse;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardWidgetResponse;
import org.graylog2.restclient.models.api.responses.dashboards.WidgetPositionResponse;
import org.graylog2.restclient.models.dashboards.widgets.DashboardWidget;
import org.graylog2.restroutes.generated.routes;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest.WidgetPosition;
import static org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest.create;

public class Dashboard {

    private static final Logger LOG = LoggerFactory.getLogger(Dashboard.class);

    public interface Factory {
        Dashboard fromSummaryResponse(DashboardSummaryResponse dsr);
    }

    private final String id;
    private final String title;
    private final String description;
    private final DateTime createdAt;
    private final User creatorUser;
    private final String contentPack;

    private final Map<String, DashboardWidget> widgets;
    private final Map<String, WidgetPositionResponse> positions;

    private final ApiClient api;

    @AssistedInject
    private Dashboard(UserService userService, ApiClient api, @Assisted DashboardSummaryResponse dsr) {
        this.id = dsr.id;
        this.title = dsr.title;
        this.description = dsr.description;
        this.createdAt = DateTime.parse(dsr.createdAt);
        this.creatorUser = userService.load(dsr.creatorUserId);
        this.api = api;
        this.positions = dsr.positions == null ? new HashMap<String, WidgetPositionResponse>() : dsr.positions;
        this.widgets = parseWidgets(dsr.widgets);
        this.contentPack = dsr.contentPack;
    }

    public void addWidget(DashboardWidget widget) throws APIException, IOException {
        AddWidgetRequest request = new AddWidgetRequest(widget);

        api.path(routes.DashboardsResource().addWidget(id))
                .onlyMasterNode()
                .body(request)
                .expect(Http.Status.CREATED)
                .execute();
    }

    public void removeWidget(String widgetId) throws APIException, IOException {
        api.path(routes.DashboardsResource().remove(id, widgetId))
                .onlyMasterNode()
                .expect(Http.Status.NO_CONTENT)
                .execute();
    }

    public void update(UpdateDashboardRequest udr) throws APIException, IOException {
        api.path(routes.DashboardsResource().update(id))
                .body(udr)
                .expect(Http.Status.OK)
                .execute();
    }

    public void setWidgetPositions(UserSetWidgetPositionsRequest request) throws APIException, IOException {
        final List<WidgetPosition> positions = Lists.newArrayList();

        for (UserWidgetPositionRequest userPosition : request.positions) {
            final WidgetPosition position = WidgetPosition.create(
                    userPosition.id,
                    userPosition.col,
                    userPosition.row,
                    userPosition.size_y,
                    userPosition.size_x
            );

            positions.add(position);
        }

        final WidgetPositionsRequest updatePositionsRequest = create(positions);

        api.path(routes.DashboardsResource().setPositions(id))
                .body(updatePositionsRequest)
                .expect(Http.Status.OK)
                .execute();
    }

    public DashboardWidget getWidget(String id) {
        return widgets.get(id);
    }

    public Map<String, DashboardWidget> getWidgets() {
        return widgets;
    }

    public Map<String, WidgetPositionResponse> getPositions() {
        return positions;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public User getCreatorUser() {
        return creatorUser;
    }

    public String getContentPack() {
        return contentPack;
    }

    private Map<String, DashboardWidget> parseWidgets(List<DashboardWidgetResponse> widgetDefinitions) {
        Map<String, DashboardWidget> widgets = Maps.newHashMap();

        for (DashboardWidgetResponse w : widgetDefinitions) {
            try {
                widgets.put(w.id, DashboardWidget.factory(this, w));
            } catch (DashboardWidget.NoSuchWidgetTypeException e) {
                LOG.error("Skipping not supported widget: [{}]", w.type, e);
                continue;
            } catch (InvalidRangeParametersException e) {
                LOG.error("Skipping widget with invalid timerange parameters: [{}]", w.id, e);
                continue;
            }
        }

        return widgets;
    }

}
