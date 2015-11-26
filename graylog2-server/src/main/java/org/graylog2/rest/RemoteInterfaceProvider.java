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
