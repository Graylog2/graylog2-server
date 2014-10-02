/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models.dashboards;

import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.graylog2.restclient.models.api.requests.dashboards.*;
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

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
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
    }

    public void addWidget(DashboardWidget widget, User user) throws APIException, IOException {
        AddWidgetRequest request = new AddWidgetRequest(widget, user);

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

    public void setWidgetPositions(List<UserWidgetPositionRequest> positions) throws APIException, IOException {
        SetWidgetPositionsRequest req = new SetWidgetPositionsRequest();

        for (UserWidgetPositionRequest userPosition : positions) {
            WidgetPositionRequest position = new WidgetPositionRequest();
            position.id = userPosition.id;
            position.col = userPosition.col;
            position.row = userPosition.row;

            req.positions.add(position);
        }

        api.path(routes.DashboardsResource().setPositions(id))
                .body(req)
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
