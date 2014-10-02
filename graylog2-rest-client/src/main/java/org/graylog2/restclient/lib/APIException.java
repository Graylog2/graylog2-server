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
