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
package org.graylog2.shared.bindings.providers;

import com.github.joschi.jadconfig.util.Duration;
import com.squareup.okhttp.OkHttpClient;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provider for a configured {@link com.squareup.okhttp.OkHttpClient}.
 *
 * @see org.graylog2.plugin.BaseConfiguration#getHttpConnectTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpReadTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpWriteTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpProxyUri()
 */
@Singleton
public class OkHttpClientProvider implements Provider<OkHttpClient> {
    protected final Duration connectTimeout;
    protected final Duration readTimeout;
    protected final Duration writeTimeout;
    protected final URI httpProxyUri;

    public OkHttpClientProvider(@Named("http_connect_timeout") Duration connectTimeout,
                                @Named("http_read_timeout") Duration readTimeout,
                                @Named("http_write_timeout") Duration writeTimeout,
                                @Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.connectTimeout = checkNotNull(connectTimeout);
        this.readTimeout = checkNotNull(readTimeout);
        this.writeTimeout = checkNotNull(writeTimeout);
        this.httpProxyUri = httpProxyUri;
    }

    @Inject


    @Override
    public OkHttpClient get() {
        final OkHttpClient client = new OkHttpClient();
        client.setRetryOnConnectionFailure(true);
        client.setConnectTimeout(connectTimeout.getQuantity(), connectTimeout.getUnit());
        client.setWriteTimeout(writeTimeout.getQuantity(), writeTimeout.getUnit());
        client.setReadTimeout(readTimeout.getQuantity(), readTimeout.getUnit());

        if (httpProxyUri != null) {
            final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyUri.getHost(), httpProxyUri.getPort()));
            client.setProxy(proxy);
        }

        return client;
    }
}
