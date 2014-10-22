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

    public void delete(String bundleId) throws APIException, IOException {
        api.path(routes.BundleResource().deleteBundle(bundleId)).expect(Http.Status.NO_CONTENT).execute();
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
