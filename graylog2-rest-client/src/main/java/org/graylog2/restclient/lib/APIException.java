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

import com.google.common.base.Splitter;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

public class APIException extends Exception {

    private final Response response;
    private final Request request;

    public APIException(Request request, Response response, Throwable cause) {
        this.request = request;
        this.response = response;
        initCause(cause);
    }

    public APIException(Request request, Response response) {
        this(request, response, null);
    }

    public APIException(Request request, Throwable cause) {
        this(request, null, cause);
    }

    public int getHttpCode() {
		return response != null ? response.getStatusCode() : -1;
	}

    @Override
    public String getMessage() {
        final StringBuilder sb = new StringBuilder();

        sb.append("API call failed");
        if (request != null) {
            sb.append(' ');
            sb.append(request.getMethod());
            sb.append(' ');
            try {
                final URI uri = request.getURI();
                final String userInfo = uri.getUserInfo();
                String username = "";
                if (userInfo != null) {
                    final Iterable<String> userSplitter = Splitter.on(':').trimResults().omitEmptyStrings().split(userInfo);
                    final Iterator<String> it = userSplitter.iterator();
                    if (it.hasNext()) {
                        username = it.next();
                    }
                }
                final URI cleanUri = new URI(uri.getScheme(), username, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                sb.append(cleanUri.toASCIIString());
            } catch (URISyntaxException e) {
                sb.append("invalid URL");
                // ignore
            }
        }
        if (response != null) {
            sb.append(" returned");
            sb.append(' ');
            sb.append(response.getStatusCode()).append(' ').append(response.getStatusText());
            try {
                String body = response.getResponseBody();
                sb.append(" body: ").append(body);
            } catch (IOException ignored) {}
        }
        return sb.toString();
    }
}
