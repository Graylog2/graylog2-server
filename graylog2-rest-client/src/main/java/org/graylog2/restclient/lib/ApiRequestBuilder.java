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

import com.google.common.net.MediaType;
import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.Radio;
import org.graylog2.restclient.models.api.requests.ApiRequest;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface ApiRequestBuilder<T> {
    ApiRequestBuilder<T> path(String pathTemplate);

    // convenience
    ApiRequestBuilder<T> path(String pathTemplate, Object... params);

    ApiRequestBuilder<T> pathParams(Object... params);

    ApiRequestBuilder<T> pathParam(Object param);

    ApiRequestBuilder<T> node(Node node);

    ApiRequestBuilder<T> radio(Radio radio);

    ApiRequestBuilder<T> clusterEntity(ClusterEntity entity);

    ApiRequestBuilder<T> nodes(Node... nodes);

    ApiRequestBuilder<T> nodes(Collection<Node> nodes);

    ApiRequestBuilder<T> fromAllNodes();

    ApiRequestBuilder<T> onlyMasterNode();

    ApiRequestBuilder<T> queryParam(String name, String value);

    ApiRequestBuilder<T> queryParam(String name, int value);

    ApiRequestBuilder<T> queryParams(Map<String, String> params);

    ApiRequestBuilder<T> session(String sessionId);

    ApiRequestBuilder<T> extendSession(boolean extend);

    ApiRequestBuilder<T> unauthenticated();

    ApiRequestBuilder<T> body(ApiRequest body);

    ApiRequestBuilder<T> expect(int... httpStatusCodes);

    ApiRequestBuilder<T> timeout(long value);

    ApiRequestBuilder<T> timeout(long value, TimeUnit unit);

    ApiRequestBuilder<T> accept(MediaType mediaType);

    T execute() throws APIException, IOException;

    Map<Node, T> executeOnAll();

    // solely for test purposes
    URL prepareUrl(ClusterEntity node);
}
