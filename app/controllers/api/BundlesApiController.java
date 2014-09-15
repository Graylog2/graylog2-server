package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.Inject;
import controllers.AuthenticatedController;
import org.glassfish.grizzly.utils.Charsets;
import org.graylog2.restclient.models.BundleService;
import org.graylog2.restclient.models.ConfigurationBundle;
import org.graylog2.restclient.models.api.requests.CreateBundleRequest;
import play.Logger;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

import java.io.File;
import java.io.IOException;

public class BundlesApiController extends AuthenticatedController {
    @Inject
    private BundleService bundleService;

    public Result index() {
        Multimap<String, ConfigurationBundle> bundles = bundleService.all();

        return ok(new Gson().toJson(bundles.asMap())).as("application/json");
    }

    public Result create() {
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
                return redirect("/");
            }
            if (bundleService.create(cbr)) {
                flash("success", "Bundle added successfully");
                return redirect("/");
            } else {
                flash("error", "There was an error adding the bundle, please try again later");
                return redirect("/");
            }
        } else {
            flash("error", "You didn't upload any bundle file");
            return redirect("/");
        }
    }
}
