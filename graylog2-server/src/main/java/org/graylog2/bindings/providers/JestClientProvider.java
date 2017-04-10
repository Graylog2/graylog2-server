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
package org.graylog2.bindings.providers;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class JestClientProvider implements Provider<JestClient> {
    private final JestClientFactory factory;
    private final CredentialsProvider credentialsProvider;

    public JestClientProvider(@Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
                              @Named("elasticsearch_connect_timeout") int elasticsearchConnectTimeout,
                              @Named("elasticsearch_socket_timeout") int elasticsearchSocketTimeout,
                              @Named("elasticsearch_max_retry_timeout") int elasticsearchMaxRetryTimeout) {
        this.factory = new JestClientFactory();
        this.credentialsProvider = new BasicCredentialsProvider();
        final List<String> hosts = elasticsearchHosts.stream()
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
                return hostUri.toString();
            })
            .collect(Collectors.toList());

        factory.setHttpClientConfig(new HttpClientConfig
            .Builder(hosts)
            .connTimeout(elasticsearchConnectTimeout)
            .readTimeout(elasticsearchSocketTimeout)
            .multiThreaded(true)
            .build());
    }

    @Override
    public JestClient get() {
        return factory.getObject();
    }
}
