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

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.system.ListPluginResponse;
import org.graylog2.restroutes.generated.routes;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PluginService {
    private final ApiClient api;

    @Inject
    public PluginService(ApiClient api) {
        this.api = api;
    }

    public List<Plugin> list(Node node) throws APIException, IOException {
        return  api.path(routes.PluginResource().list(), ListPluginResponse.class).node(node).execute().plugins;
    }
}
