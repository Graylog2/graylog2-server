/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.bindings.providers;

import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.graylog2.utilities.ProxyHostsPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

public class ProxySelectorProvider implements Provider<ProxySelector> {
    private static final Logger LOG = LoggerFactory.getLogger(ProxySelectorProvider.class);

    protected final URI httpProxyUri;
    protected final ProxyHostsPattern nonProxyHostsPattern;

    @Inject
    public ProxySelectorProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri,
                                 @Named("http_non_proxy_hosts") @Nullable ProxyHostsPattern nonProxyHostsPattern) {
        this.httpProxyUri = httpProxyUri;
        this.nonProxyHostsPattern = nonProxyHostsPattern;
    }

    @Override
    public ProxySelector get() {
        if (httpProxyUri == null) {
            return ProxySelector.getDefault();
        }
        return new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                final String host = uri.getHost();
                if (nonProxyHostsPattern != null && nonProxyHostsPattern.matches(host)) {
                    LOG.debug("Bypassing proxy server for {}", host);
                    return ImmutableList.of(Proxy.NO_PROXY);
                }
                try {
                    final InetAddress targetAddress = InetAddress.getByName(host);
                    if (targetAddress.isLoopbackAddress()) {
                        return ImmutableList.of(Proxy.NO_PROXY);
                    } else if (nonProxyHostsPattern != null && nonProxyHostsPattern.matches(targetAddress.getHostAddress())) {
                        LOG.debug("Bypassing proxy server for {}", targetAddress.getHostAddress());
                        return ImmutableList.of(Proxy.NO_PROXY);
                    }
                } catch (UnknownHostException e) {
                    LOG.debug("Unable to resolve host name for proxy selection: ", e);
                }

                final Proxy proxy = new Proxy(Proxy.Type.HTTP, getProxyAddress());
                return ImmutableList.of(proxy);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                LOG.warn("Unable to connect to proxy: ", ioe);
            }
        };
    }

    public InetSocketAddress getProxyAddress() {
        return new InetSocketAddress(httpProxyUri.getHost(), httpProxyUri.getPort());
    }
}
