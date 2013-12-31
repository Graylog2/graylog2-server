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
package controllers;

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.BreadcrumbList;
import models.api.requests.dashboards.UpdateDashboardRequest;
import models.dashboards.Dashboard;
import models.dashboards.DashboardService;
import models.NodeService;
import models.api.requests.dashboards.CreateDashboardRequest;
import play.Logger;
import play.data.Form;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DashboardsController extends AuthenticatedController {

    private static final Form<CreateDashboardRequest> createDashboardForm = Form.form(CreateDashboardRequest.class);

    @Inject
    private NodeService nodeService;

    @Inject
    private DashboardService dashboardService;

    public Result index() {
        try {
            List<Dashboard> dashboards = dashboardService.getAll();

            return ok(views.html.dashboards.index.render(currentUser(), dashboards));
        } catch (APIException e) {
            String message = "Could not get dashboards. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result show(String id) {
        try {
            Dashboard dashboard = dashboardService.get(id);

            final BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("Dashboards", routes.DashboardsController.index());
            bc.addCrumb(dashboard.getTitle(), routes.DashboardsController.show(dashboard.getId()));

            return ok(views.html.dashboards.show.render(currentUser(), bc, dashboard));
        } catch (APIException e) {
            String message = "Could not get dashboard. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result newDashboard() {
        final BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("Dashboards", routes.DashboardsController.index());
        bc.addCrumb("Create", routes.DashboardsController.newDashboard());

        return ok(views.html.dashboards.new_dashboard.render(currentUser(), bc, createDashboardForm));
    }

    public Result create() {
        Form<CreateDashboardRequest> form = createDashboardForm.bindFromRequest();
        if (form.hasErrors()) {
            flash("error", "Please fill out all fields");
            return redirect(routes.DashboardsController.newDashboard());
        }

        try {
            CreateDashboardRequest cdr = form.get();
            cdr.creatorUserId = currentUser().getName();
            dashboardService.create(cdr);
        } catch (APIException e) {
            String message = "Could not create dashboard. We expected HTTP 201, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }

        return redirect(routes.DashboardsController.index());
    }

    public Result update(String id) {
        Map<String,String> params = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

        UpdateDashboardRequest udr = new UpdateDashboardRequest();
        udr.title = params.get("title");
        udr.description = params.get("description");

        try {
            Dashboard dashboard = dashboardService.get(id);
            dashboard.update(udr);
        } catch (APIException e) {
            String message = "Could not update dashboard. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }

        return redirect(routes.DashboardsController.show(id));
    }

    public Result delete(String id) {
        try {
            dashboardService.delete(id);
            return redirect(routes.DashboardsController.index());
        } catch (APIException e) {
            String message = "Could not delete dashboard. We expected HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

}
