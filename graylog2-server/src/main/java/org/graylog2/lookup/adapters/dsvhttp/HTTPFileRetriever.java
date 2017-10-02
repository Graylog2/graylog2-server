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
package org.graylog2.lookup.adapters.dsvhttp;

import com.google.common.collect.ImmutableMap;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class HTTPFileRetriever {
    private final AtomicReference<Map<String, String>> lastLastModified = new AtomicReference<>(Collections.emptyMap());
    private final OkHttpClient client;

    @Inject
    public HTTPFileRetriever(OkHttpClient httpClient) {
        this.client = httpClient.newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public Optional<String> fetchFileIfNotModified(String url) throws IOException {
        final Request.Builder requestBuilder = new Request.Builder()
                .get()
                .url(url)
                .header("User-Agent", "Graylog (server)");
        final String lastModified = this.lastLastModified.get().get(url);
        if (lastModified != null) {
            requestBuilder.header("If-Modified-Since", lastModified);
        }
        final Call request = client.newCall(requestBuilder.build());

        try (final Response response = request.execute()) {
            if (response.isSuccessful()) {
                final String lastModifiedHeader = response.header("Last-Modified", DateTime.now(DateTimeZone.UTC).toString());
                final Map<String, String> newLastModified = new HashMap<>(this.lastLastModified.get());
                newLastModified.put(url, lastModifiedHeader);
                this.lastLastModified.set(ImmutableMap.copyOf(newLastModified));

                if (response.body() != null) {
                    final String body = response.body().string();
                    return Optional.ofNullable(body);
                }
            } else {
                if (response.code() != 304) {
                    throw new IOException("Request failed: " + response.message());
                }
            }
        }

        return Optional.empty();
    }
}
