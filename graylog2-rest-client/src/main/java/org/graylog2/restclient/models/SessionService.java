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

import org.graylog2.rest.models.system.sessions.requests.SessionCreateRequest;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restroutes.generated.routes;

import javax.inject.Inject;

import static play.mvc.Http.Status.*;

import java.io.IOException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SessionService {

    private final ApiClient apiClient;

    @Inject
    public SessionService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public SessionResponse create(String username, String password, String remoteAddress) throws APIException, IOException {
        return apiClient.path(routes.SessionsResource().newSession(), SessionResponse.class)
                .unauthenticated()
                .body(SessionCreateRequest.create(username, password, remoteAddress))
                .execute();
    }

    public void destroy(String sessionId) throws APIException, IOException {
        apiClient.path(routes.SessionsResource().terminateSession(sessionId))
                .expect(NO_CONTENT, NOT_FOUND)
                .execute();
    }
}
