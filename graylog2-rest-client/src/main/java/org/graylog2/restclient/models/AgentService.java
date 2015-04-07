/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.models;

import org.graylog2.rest.models.agent.responses.AgentList;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restroutes.generated.routes;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class AgentService {
    private final ApiClient apiClient;

    @Inject
    public AgentService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<AgentSummary> all() throws APIException, IOException {
        final AgentList response = apiClient.path(routes.AgentResource().list(), AgentList.class).execute();
        return response.agents();
    }
}
