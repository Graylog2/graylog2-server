package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.SessionCreateRequest;
import org.graylog2.restclient.models.api.responses.SessionCreateResponse;
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

    public SessionCreateResponse create(String username, String password, String remoteAddress) throws APIException, IOException {
        return apiClient.path(routes.SessionsResource().newSession(), SessionCreateResponse.class)
                .unauthenticated()
                .body(new SessionCreateRequest(username, password, remoteAddress))
                .execute();
    }

    public void destroy(String sessionId) throws APIException, IOException {
        apiClient.path(routes.SessionsResource().terminateSession(sessionId))
                .expect(NO_CONTENT, NOT_FOUND)
                .execute();
    }
}
