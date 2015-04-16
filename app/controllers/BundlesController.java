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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.Tools;
import org.graylog2.restclient.models.*;
import org.graylog2.restclient.models.api.requests.ExportBundleRequest;
import org.graylog2.restclient.models.bundles.BundleService;
import org.graylog2.restclient.models.bundles.ConfigurationBundle;
import org.graylog2.restclient.models.dashboards.Dashboard;
import org.graylog2.restclient.models.dashboards.DashboardService;
import play.Logger;
import play.data.Form;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class BundlesController extends AuthenticatedController {
    private BundleService bundleService;
    private InputService inputService;
    private OutputService outputService;
    private StreamService streamService;
    private DashboardService dashboardService;

    final Form<ExportBundleRequest> exportBundleForm = Form.form(ExportBundleRequest.class);

    @Inject
    public BundlesController(BundleService bundleService,
                             InputService inputService,
                             OutputService outputService,
                             StreamService streamService,
                             DashboardService dashboardService) {
        this.bundleService = bundleService;
        this.inputService = inputService;
        this.outputService = outputService;
        this.streamService = streamService;
        this.dashboardService = dashboardService;
    }

    public Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Content packs", routes.BundlesController.index());
        return ok(views.html.system.bundles.index.render(currentUser(), bc));
    }

    public Result exportForm()  {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Content packs", routes.BundlesController.index());
        bc.addCrumb("Create", routes.BundlesController.exportForm());

        final Map<String, List> data = getListData();

        return ok(views.html.system.bundles.export.render(
                exportBundleForm,
                currentUser(),
                bc,
                (List<Input>) data.get("inputs"),
                (List<Output>) data.get("outputs"),
                (List<Stream>) data.get("streams"),
                (List<Dashboard>) data.get("dashboards")
        ));
    }

    public Result export() {
        final Form<ExportBundleRequest> form = Tools.bindMultiValueFormFromRequest(ExportBundleRequest.class);

        if (form.hasErrors()) {
            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Content packs", routes.BundlesController.index());
            bc.addCrumb("Create", routes.BundlesController.exportForm());

            Map<String, List> data = getListData();

            flash("error", "Please correct the fields marked in red to export the bundle");
            return badRequest(views.html.system.bundles.export.render(
                    form,
                    currentUser(),
                    bc,
                    (List<Input>) data.get("inputs"),
                    (List<Output>) data.get("outputs"),
                    (List<Stream>) data.get("streams"),
                    (List<Dashboard>) data.get("dashboards")
            ));
        }

        try {
            final ExportBundleRequest exportBundleRequest = form.get();
            ConfigurationBundle bundle = bundleService.export(exportBundleRequest);

            response().setContentType(MediaType.JSON_UTF_8.toString());
            response().setHeader("Content-Disposition", "attachment; filename=content_pack.json");
            ObjectMapper m = new ObjectMapper();
            ObjectWriter ow = m.writer().withDefaultPrettyPrinter();
            return ok(ow.writeValueAsString(bundle));
        } catch (IOException e) {
            flash("error", "Could not reach Graylog server");
        } catch (Exception e) {
            flash("error", "Unexpected error exporting configuration bundle, please try again later");
        }

        return redirect(routes.BundlesController.exportForm());
    }

    private Map<String, List> getListData() {
        final Map<String, List> data = Maps.newHashMap();

        try {
            final List<Input> inputs = Lists.newArrayList();
            for (InputState inputState : inputService.loadAllInputStates()) {
                inputs.add(inputState.getInput());
            }
            data.put("inputs", inputs);
            data.put("outputs", outputService.list());
            data.put("streams", streamService.all());
            data.put("dashboards", dashboardService.getAll());
        } catch (APIException e) {
            Logger.error("Could not fetch data. We expected HTTP 200, but got a HTTP " + e.getHttpCode());
        } catch (IOException e) {
            Logger.error("Could not connect to Graylog server. " + e);
        }

        return data;
    }
}
