package org.graylog2.restclient.models;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.CreateBundleRequest;
import org.graylog2.restroutes.generated.BundleResource;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;

public class BundleService {
    private static final Logger LOG = LoggerFactory.getLogger(BundleService.class);

    private final ApiClient api;
    private final BundleResource resource = routes.BundleResource();

    @Inject
    public BundleService(ApiClient api) {
        this.api = api;
    }

    public Multimap<String, ConfigurationBundle> all() {
        try {
            Multimap<String, ConfigurationBundle> response = api.path(routes.BundleResource().listBundles(), Multimap.class).execute();
            return response;
        } catch (APIException e) {
            LOG.error("Unable to get bundle list from server", e);
        } catch (IOException e) {
            LOG.error("Unable to read from server socket", e);
        }
        return ArrayListMultimap.create();
    }

    public boolean create(CreateBundleRequest request) {
        try {
            api.path(resource.createBundle()).body(request).expect(Http.Status.CREATED).execute();
            return true;
        } catch (APIException e) {
            LOG.error("Unable to create bundle", e);
            return false;
        } catch (IOException e) {
            LOG.error("Unable to create bundle", e);
            return false;
        }
    }

    public void apply(String bundleId) throws APIException, IOException {
        api.path(resource.applyBundle(bundleId)).expect(Http.Status.NO_CONTENT).execute();
    }
}
