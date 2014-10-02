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
