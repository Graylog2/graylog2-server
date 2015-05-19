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
package controllers;

import com.google.inject.Inject;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import lib.BreadcrumbList;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Startpage;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.api.requests.dashboards.CreateDashboardRequest;
import org.graylog2.restclient.models.api.requests.dashboards.UpdateDashboardRequest;
import org.graylog2.restclient.models.dashboards.Dashboard;
import org.graylog2.restclient.models.dashboards.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.mvc.BodyParser;
import play.mvc.Result;
import views.helpers.Permissions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DashboardsController extends AuthenticatedController {
    private static final Logger log = LoggerFactory.getLogger(DashboardsController.class);

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
        final User currentUser = currentUser();
        try {
            Dashboard dashboard = dashboardService.get(id);

            final BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("Dashboards", routes.DashboardsController.index());
            bc.addCrumb(dashboard.getTitle(), routes.DashboardsController.show(dashboard.getId()));

            return ok(views.html.dashboards.show.render(currentUser, bc, dashboard));
        } catch (APIException e) {
            if (e.getHttpCode() == NOT_FOUND || e.getHttpCode() == FORBIDDEN) {
                String msg = "The requested dashboard was deleted and no longer exists.";
                final Startpage startpage = currentUser.getStartpage();
                if (startpage != null) {
                    if (new Startpage(Startpage.Type.DASHBOARD, id).equals(startpage)) {
                        msg += " Please reset your startpage.";
                    }
                }
                flash("error", msg);
                return redirect(routes.DashboardsController.index());
            }
            String message = "Could not get dashboard. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result newDashboard() {
        if (!Permissions.isPermitted(RestPermissions.DASHBOARDS_CREATE)) {
            return redirect(routes.StartpageController.redirect());
        }
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
            dashboardService.create(cdr);
        } catch (APIException e) {
            String message = "Could not create dashboard. We expected HTTP 201, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }

        return redirect(routes.DashboardsController.index());
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
