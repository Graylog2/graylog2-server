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
import com.google.common.collect.ImmutableList;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Provider for a configured {@link okhttp3.OkHttpClient}.
 *
 * @see org.graylog2.plugin.BaseConfiguration#getHttpConnectTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpReadTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpWriteTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpProxyUri()
 */
@Singleton
public class OkHttpClientProvider implements Provider<OkHttpClient> {
    private static final Logger LOG = LoggerFactory.getLogger(OkHttpClientProvider.class);
    protected final Duration connectTimeout;
    protected final Duration readTimeout;
    protected final Duration writeTimeout;
    protected final URI httpProxyUri;

    @Inject
    public OkHttpClientProvider(@Named("http_connect_timeout") Duration connectTimeout,
                                @Named("http_read_timeout") Duration readTimeout,
                                @Named("http_write_timeout") Duration writeTimeout,
                                @Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.connectTimeout = requireNonNull(connectTimeout);
        this.readTimeout = requireNonNull(readTimeout);
        this.writeTimeout = requireNonNull(writeTimeout);
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public OkHttpClient get() {
        final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(connectTimeout.getQuantity(), connectTimeout.getUnit())
                .writeTimeout(writeTimeout.getQuantity(), writeTimeout.getUnit())
                .readTimeout(readTimeout.getQuantity(), readTimeout.getUnit());

        if (httpProxyUri != null) {
            final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyUri.getHost(), httpProxyUri.getPort()));
            final ProxySelector proxySelector = new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    try {
                        final InetAddress targetAddress = InetAddress.getByName(uri.getHost());
                        if (targetAddress.isLoopbackAddress()) {
                            return ImmutableList.of(Proxy.NO_PROXY);
                        }
                    } catch (UnknownHostException e) {
                        LOG.debug("Unable to resolve host name for proxy selection: ", e);
                    }
                    return ImmutableList.of(proxy);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    LOG.warn("Unable to connect to proxy: ", ioe);
                }
            };

            clientBuilder.proxySelector(proxySelector);
        }

        return clientBuilder.build();
    }
}
