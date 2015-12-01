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
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.graylog2.cluster.Node;
import retrofit.JacksonConverterFactory;
import retrofit.Retrofit;

import javax.inject.Inject;
import java.io.IOException;

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
        final OkHttpClient okHttpClient = this.okHttpClient.clone();
        okHttpClient.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                final Request original = chain.request();

                Request.Builder builder = original.newBuilder()
                        .header("Accept", "application/json")
                        .method(original.method(), original.body());

                if (authorizationToken != null) {
                    builder = builder.header("Authorization", authorizationToken);
                }

                return chain.proceed(builder.build());
            }
        });
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
