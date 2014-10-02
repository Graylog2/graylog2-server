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
package org.graylog2.restclient.models.bundles;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.CreateBundleRequest;
import org.graylog2.restclient.models.api.requests.ExportBundleRequest;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;

public class BundleService {
    private static final Logger LOG = LoggerFactory.getLogger(BundleService.class);

    private final ApiClient api;

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
            LOG.error("Unable to communicate with Graylog2 server", e);
        }
        return ArrayListMultimap.create();
    }

    public boolean create(CreateBundleRequest request) {
        try {
            api.path(routes.BundleResource().createBundle()).body(request).expect(Http.Status.CREATED).execute();
            return true;
        } catch (APIException e) {
            LOG.error("Unable to create bundle", e);
            return false;
        } catch (IOException e) {
            LOG.error("Unable to communicate with Graylog2 server", e);
            return false;
        }
    }

    public void apply(String bundleId) throws APIException, IOException {
        api.path(routes.BundleResource().applyBundle(bundleId)).expect(Http.Status.NO_CONTENT).execute();
    }

    public ConfigurationBundle export(ExportBundleRequest request) throws APIException, IOException {
        try {
            ConfigurationBundle response = api.path(routes.BundleResource().exportBundle(), ConfigurationBundle.class)
                    .body(request)
                    .expect(Http.Status.OK)
                    .execute();
            return response;
        } catch (APIException e) {
            LOG.error("Unable to export bundle", e);
            throw e;
        } catch (IOException e) {
            LOG.error("Unable to communicate with Graylog2 server", e);
            throw e;
        }
    }
}
