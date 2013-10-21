/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.inputs.misc.jsonpath;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Collector {

    private static final Logger LOG = LoggerFactory.getLogger(Collector.class);

    private final String url;
    private final Map<String, String> headers;
    private final String inputId;

    public Collector(String url,  Map<String, String> headers, String inputId) {
        this.url = url;
        this.headers = headers;
        this.inputId = inputId;
    }

    public String getJson() throws InterruptedException, ExecutionException, IOException {
        LOG.debug("Getting JSON for JsonPathInput <{}> from [{}].", inputId, url);

        AsyncHttpClient client = new AsyncHttpClient();
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url);

            // Add custom headers if there are some.
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                }
            }

            Response r = requestBuilder.execute().get();

            if (r.getStatusCode() != 200) {
                throw new RuntimeException("Expected HTTP status code 200, got " + r.getStatusCode());
            }

            return r.getResponseBody();
        } finally {
            client.close();
        }
    }

}
