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
