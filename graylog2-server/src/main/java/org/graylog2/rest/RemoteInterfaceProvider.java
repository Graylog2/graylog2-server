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
package org.graylog2.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.graylog2.cluster.Node;
import org.graylog2.security.realm.SessionAuthenticator;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.inject.Inject;

public class RemoteInterfaceProvider {
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    @Inject
    public RemoteInterfaceProvider(ObjectMapper objectMapper,
                                   OkHttpClient okHttpClient) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
    }

    public <T> T get(Node node, final String authorizationToken, Class<T> interfaceClass) {
        final OkHttpClient okHttpClient = this.okHttpClient.newBuilder()
                .addInterceptor(chain -> {
                    final Request original = chain.request();

                    Request.Builder builder = original.newBuilder()
                            .header(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString())
                            .method(original.method(), original.body());

                    if (authorizationToken != null) {
                        builder = builder
                                // forward the authentication information of the current user
                                .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                                // do not extend the users session with proxied requests
                                .header(SessionAuthenticator.X_GRAYLOG_NO_SESSION_EXTENSION, "true");
                    }

                    return chain.proceed(builder.build());
                })
                .build();
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(node.getTransportAddress())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .client(okHttpClient)
                .build();

        return retrofit.create(interfaceClass);
    }

    public <T> T get(Node node, Class<T> interfaceClass) {
        return get(node, null, interfaceClass);
    }
}
