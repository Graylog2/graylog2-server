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
package org.graylog2.restclient.lib;

import com.google.inject.ImplementedBy;
import com.ning.http.client.AsyncHttpClient;
import org.graylog2.restclient.models.api.responses.EmptyResponse;
import org.graylog2.restroutes.PathMethod;

/*
  Note: This is an interface to break the cyclic dependency between ServerNodes and ApiClientImpl.
  Guice will insert a proxy to break the cycle.
 */
@ImplementedBy(ApiClientImpl.class)
public interface ApiClient {
    String ERROR_MSG_IO = "Could not connect to graylog2-server. Please make sure that it is running and you configured the correct REST URI.";
    String ERROR_MSG_NODE_NOT_FOUND = "Node not found.";

    void start();

    void stop();

    // default visibility for access from tests (overrides the effects of initialize())
    void setHttpClient(AsyncHttpClient client);

    <T> ApiRequestBuilder<T> get(Class<T> responseClass);

    <T> ApiRequestBuilder<T> post(Class<T> responseClass);

    ApiRequestBuilder<EmptyResponse> post();

    <T> ApiRequestBuilder<T> put(Class<T> responseClass);

    ApiRequestBuilder<EmptyResponse> put();

    <T> ApiRequestBuilder<T> delete(Class<T> responseClass);

    ApiRequestBuilder<EmptyResponse> delete();

    <T> ApiRequestBuilder<T> path(PathMethod pathMethod, Class<T> responseClasse);

    ApiRequestBuilder<EmptyResponse> path(PathMethod pathMethod);

    public enum Method {
        GET,
        POST,
        PUT,
        DELETE
    }
}
