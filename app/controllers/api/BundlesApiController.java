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
package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.Inject;
import controllers.AuthenticatedController;
import org.glassfish.grizzly.utils.Charsets;
import org.graylog2.restclient.models.bundles.BundleService;
import org.graylog2.restclient.models.bundles.ConfigurationBundle;
import org.graylog2.restclient.models.api.requests.CreateBundleRequest;
import play.Logger;
import play.Play;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class BundlesApiController extends AuthenticatedController {
    @Inject
    private BundleService bundleService;

    public Result index() {
        Multimap<String, ConfigurationBundle> bundles = bundleService.all();

        return ok(new Gson().toJson(bundles.asMap())).as("application/json");
    }

    public Result create() {
        String path = getRefererPath();
        MultipartFormData body = request().body().asMultipartFormData();
        FilePart bundle = body.getFile("bundle");
        if (bundle != null) {
            CreateBundleRequest cbr;
            try {
                File file = bundle.getFile();
                String bundleContents = Files.toString(file, Charsets.UTF8_CHARSET);
                ObjectMapper om = new ObjectMapper();
                cbr = om.readValue(bundleContents, CreateBundleRequest.class);
            } catch (IOException e) {
                Logger.error("Could not parse uploaded bundle: " + e);
                flash("error", "The uploaded bundle could not be applied: does it have the right format?");
                return redirect(path);
            }
            if (bundleService.create(cbr)) {
                flash("success", "Bundle added successfully");
            } else {
                flash("error", "There was an error adding the bundle, please try again later");
            }
        } else {
            flash("error", "You didn't upload any bundle file");
        }
        return redirect(path);
    }

    public Result apply(String bundleId) {
        try {
            bundleService.apply(bundleId);
            flash("success", "Bundle applied successfully");
        } catch (Exception e) {
            flash("error", "Could not apply bundle: " + e);
        }
        return redirect(getRefererPath());
    }

    private String getRefererPath() {
        try {
            URL parser = new URL(request().getHeader(REFERER));
            return parser.getPath();
        } catch (MalformedURLException e) {
            return "/";
        }
    }
}
