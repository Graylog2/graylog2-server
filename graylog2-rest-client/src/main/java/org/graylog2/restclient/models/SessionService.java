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
