package org.graylog2.bindings.providers;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class EsRestClientProvider implements Provider<RestClient> {

    private final List<URI> elasticsearchHosts;
    private final int elasticsearchConnectTimeout;
    private final int elasticsearchSocketTimeout;
    private final int elasticsearchMaxRetryTimeout;

    @Inject
    public EsRestClientProvider(@Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
                                @Named("elasticsearch_connect_timeout") int elasticsearchConnectTimeout,
                                @Named("elasticsearch_socket_timeout") int elasticsearchSocketTimeout,
                                @Named("elasticsearch_max_retry_timeout") int elasticsearchMaxRetryTimeout) {
        this.elasticsearchHosts = elasticsearchHosts;
        this.elasticsearchConnectTimeout = elasticsearchConnectTimeout;
        this.elasticsearchSocketTimeout = elasticsearchSocketTimeout;
        this.elasticsearchMaxRetryTimeout = elasticsearchMaxRetryTimeout;
    }

    @Override
    public RestClient get() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final List<HttpHost> httpHosts = elasticsearchHosts.stream()
            .map(hostUri -> {
                if (!Strings.isNullOrEmpty(hostUri.getUserInfo())) {
                    final Iterator<String> splittedUserInfo = Splitter.on(":")
                        .split(hostUri.getUserInfo())
                        .iterator();
                    if (splittedUserInfo.hasNext()) {
                        final String username = splittedUserInfo.next();
                        final String password = splittedUserInfo.hasNext() ? splittedUserInfo.next() : null;
                        credentialsProvider.setCredentials(
                            new AuthScope(hostUri.getHost(), hostUri.getPort(), AuthScope.ANY_REALM, hostUri.getScheme()),
                            new UsernamePasswordCredentials(username, password)
                        );
                    }
                }
                return hostUri;
            })
            .map(hostUri -> new HttpHost(hostUri.getHost(), hostUri.getPort(), hostUri.getScheme()))
            .collect(Collectors.toList());

        return RestClient.builder(httpHosts.toArray(new HttpHost[]{}))
            .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(elasticsearchConnectTimeout)
                .setSocketTimeout(elasticsearchSocketTimeout))
            .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
            .setMaxRetryTimeoutMillis(elasticsearchMaxRetryTimeout)
            .build();
    }
}
