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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.dashboards.CreateDashboardRequest;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardSummaryResponse;
import org.graylog2.restclient.models.api.responses.dashboards.GetDashboardsResponse;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
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

    public void create(CreateDashboardRequest request) throws APIException, IOException {
        api.path(routes.DashboardsResource().create())
                .onlyMasterNode()
                .body(request)
                .expect(Http.Status.CREATED)
                .execute();
    }

    public void delete(String id) throws APIException, IOException {
        api.path(routes.DashboardsResource().delete(id))
                .onlyMasterNode()
                .expect(Http.Status.NO_CONTENT)
                .execute();
    }

}
