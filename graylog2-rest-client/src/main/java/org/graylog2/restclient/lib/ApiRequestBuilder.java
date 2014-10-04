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
