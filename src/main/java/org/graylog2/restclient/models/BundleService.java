package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.CreateBundleRequest;
import org.graylog2.restclient.models.api.responses.bundles.BundleListResponse;
import org.graylog2.restroutes.generated.BundleResource;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BundleService {
    private static final Logger log = LoggerFactory.getLogger(BundleService.class);

    private final ApiClient api;
    private final BundleResource resource = routes.BundleResource();

    @Inject
    public BundleService(ApiClient api) {
        this.api = api;
    }

    public List<Map<String, String>> all() {
        List<Map<String, String>> bundles = Lists.newArrayList();
        try {
            BundleListResponse response = api.path(routes.BundleResource().listBundles(), BundleListResponse.class).execute();
        }catch (APIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bundles;
    }

    public boolean create(CreateBundleRequest request) {
        try {
            api.path(resource.createBundle()).body(request).expect(Http.Status.CREATED).execute();
            return true;
        } catch (APIException e) {
            log.error("Unable to create bundle", e);
            return false;
        } catch (IOException e) {
            log.error("Unable to create bundle", e);
            return false;
        }
    }
}
