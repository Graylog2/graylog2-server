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
import org.graylog2.rest.models.dashboards.responses.CreateDashboardResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.dashboards.CreateDashboardRequest;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardSummaryResponse;
import org.graylog2.restclient.models.api.responses.dashboards.GetDashboardsResponse;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class DashboardService {

    private final ApiClient api;
    private final Dashboard.Factory dashboardFactory;

    @Inject
    private DashboardService(ApiClient api, Dashboard.Factory dashboardFactory) {
        this.api = api;
        this.dashboardFactory = dashboardFactory;
    }


    public Dashboard get(String id) throws APIException, IOException {
        DashboardSummaryResponse d = api.path(routes.DashboardsResource().get(id), DashboardSummaryResponse.class)
                .onlyMasterNode()
                .execute();
        return dashboardFactory.fromSummaryResponse(d);
    }

    public List<Dashboard> getAll() throws APIException, IOException {
        List<Dashboard> dashboards = Lists.newArrayList();
        GetDashboardsResponse response = api.path(routes.DashboardsResource().list(), GetDashboardsResponse.class)
                .onlyMasterNode()
                .execute();

        if (response == null || response.dashboards == null) {
            return dashboards;
        }

        for(DashboardSummaryResponse dr : response.dashboards) {
            dashboards.add(dashboardFactory.fromSummaryResponse(dr));
        }

        return dashboards;
    }

    public String create(CreateDashboardRequest request) throws APIException, IOException {
        CreateDashboardResponse response = api.path(routes.DashboardsResource().create(), CreateDashboardResponse.class)
                .onlyMasterNode()
                .body(request)
                .expect(Http.Status.CREATED)
                .execute();

        return response.dashboardId();
    }

    public void delete(String id) throws APIException, IOException {
        api.path(routes.DashboardsResource().delete(id))
                .onlyMasterNode()
                .expect(Http.Status.NO_CONTENT)
                .execute();
    }

}
