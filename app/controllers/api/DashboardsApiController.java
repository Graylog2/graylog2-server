/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package controllers.api;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import controllers.AuthenticatedController;
import lib.APIException;
import lib.ApiClient;
import models.Dashboard;
import models.DashboardService;
import models.NodeService;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DashboardsApiController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;

    @Inject
    private DashboardService dashboardService;

    public Result index() {
        try {
            Map<String, Object> result = Maps.newHashMap();
            for (Dashboard d : dashboardService.getAll()) {
                Map<String, String> dashboard = Maps.newHashMap();

                dashboard.put("title", d.getTitle());
                dashboard.put("description", d.getDescription());
                dashboard.put("created_by", d.getCreatorUser().getName());

                result.put(d.getId(), dashboard);
            }

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (APIException e) {
            String message = "Could not get dashboards. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

}
