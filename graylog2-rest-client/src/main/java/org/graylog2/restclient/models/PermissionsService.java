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
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.ReaderPermissionsResponse;
import org.graylog2.restclient.models.api.responses.RestPermissionsResponse;
import org.graylog2.restroutes.generated.routes;

import java.io.IOException;
import java.util.List;

/**
 * Combines the permissions of both the server and the web-interface.
 *
 * This does not take into account different server versions yet!
 */
public class PermissionsService {

    private final ApiClient api;

    @Inject
    private PermissionsService(ApiClient api) {
        this.api = api;
    }

    public List<String> all() {
        List<String> permissions = Lists.newArrayList();
        try {
            RestPermissionsResponse response = api.path(routes.SystemResource().permissions(), RestPermissionsResponse.class).execute();
            for (String group : response.permissions.keySet()) {
                permissions.add(group + ":*");
                for (String action : response.permissions.get(group)) {
                    permissions.add(group + ":" + action);
                }
            }
        } catch (APIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return permissions;
    }

    public List<String> readerPermissions(String username) {
        List<String> permissions = Lists.newArrayList();

        try {
            final ReaderPermissionsResponse response = api
                    .path(routes.SystemResource().readerPermissions(username), ReaderPermissionsResponse.class)
                    .execute();
            permissions.addAll(response.permissions);
        } catch (APIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return permissions;
    }

}
