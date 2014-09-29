/**
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
 */
package controllers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
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
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static play.data.Form.form;

public class BundlesController extends AuthenticatedController {
    private BundleService bundleService;
    private InputService inputService;
    private OutputService outputService;
    private StreamService streamService;
    private DashboardService dashboardService;

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
        bc.addCrumb("Bundles", routes.BundlesController.index());
        return ok(views.html.system.bundles.index.render(currentUser(), bc));
    }

    public Result exportForm()  {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Bundles", routes.BundlesController.index());
        bc.addCrumb("Export", routes.BundlesController.export());

        final List<Input> inputs = Lists.newArrayList();
        List<Output> outputs = Lists.newArrayList();
        List<Stream> streams = Lists.newArrayList();
        List<Dashboard> dashboards = Lists.newArrayList();
        try {
            for (InputState inputState : inputService.loadAllInputStates()) {
                inputs.add(inputState.getInput());
            }
            outputs = outputService.list();
            streams = streamService.all();
            dashboards = dashboardService.getAll();
        } catch (APIException e) {
            Logger.error("Could not fetch data. We expected HTTP 200, but got a HTTP " + e.getHttpCode());
        } catch (IOException e) {
            Logger.error("Could not connect to Graylog2 server. " + e);
        }

        return ok(views.html.system.bundles.export.render(currentUser(), bc, inputs, outputs, streams, dashboards));
    }

    public Result export() {
        final Form<ExportBundleRequest> form = Tools.bindMultiValueFormFromRequest(ExportBundleRequest.class);

        if (form.hasErrors()) {
            flash("Alles scheisse oida");
            return redirect(routes.BundlesController.exportForm());
        }

        try {
            final ExportBundleRequest exportBundleRequest = form.get();
            ConfigurationBundle bundle = bundleService.export(exportBundleRequest);

            response().setContentType(MediaType.JSON_UTF_8.toString());
            response().setHeader("Content-Disposition", "attachment; filename=exported_bundle.json");
            ObjectMapper m = new ObjectMapper();
            ObjectWriter ow = m.writer().withDefaultPrettyPrinter();
            return ok(ow.writeValueAsString(bundle));
        } catch (IOException e) {
            flash("error", "Could not reach Graylog2 server");
        } catch (Exception e) {
            flash("error", "Unexpected error exporting configuration bundle, please try again later");
        }

        return redirect(routes.BundlesController.exportForm());
    }
}
