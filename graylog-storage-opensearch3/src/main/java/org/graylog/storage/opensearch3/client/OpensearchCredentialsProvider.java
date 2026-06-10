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
package org.graylog.storage.opensearch3.client;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.graylog2.configuration.IndexerHosts;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

public class OpensearchCredentialsProvider implements Provider<CredentialsProvider> {
    private final List<URI> openSearchHosts;
    private final String defaultUserForDiscoveredNodes;
    private final String defaultPasswordForDiscoveredNodes;
    private final boolean discoveryEnabled;

    @Inject
    public OpensearchCredentialsProvider(@IndexerHosts List<URI> openSearchHosts,
                                         @Named("elasticsearch_discovery_default_user") @Nullable String defaultUserForDiscoveredNodes,
                                         @Named("elasticsearch_discovery_default_password") @Nullable String defaultPasswordForDiscoveredNodes,
                                         @Named("elasticsearch_discovery_enabled") boolean discoveryEnabled) {
        this.openSearchHosts = openSearchHosts;
        this.defaultUserForDiscoveredNodes = defaultUserForDiscoveredNodes;
        this.defaultPasswordForDiscoveredNodes = defaultPasswordForDiscoveredNodes;
        this.discoveryEnabled = discoveryEnabled;
    }

    @Override
    public CredentialsProvider get() {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        openSearchHosts
                .forEach(hostUri -> {
                    if (!Strings.isNullOrEmpty(hostUri.getUserInfo())) {
                        final Iterator<String> splittedUserInfo = Splitter.on(":")
                                .split(hostUri.getUserInfo())
                                .iterator();
                        if (splittedUserInfo.hasNext()) {
                            final String username = splittedUserInfo.next();
                            final String password = splittedUserInfo.hasNext() ? splittedUserInfo.next() : null;
                            credentialsProvider.setCredentials(
                                    new AuthScope(hostUri.getHost(), hostUri.getPort()),
                                    new UsernamePasswordCredentials(username, password.toCharArray())
                            );
                        }
                    }
                });

        if (discoveryEnabled && !Strings.isNullOrEmpty(defaultUserForDiscoveredNodes) && !Strings.isNullOrEmpty(defaultPasswordForDiscoveredNodes)) {
            credentialsProvider.setCredentials(
                    new AuthScope(null, -1), // any host, any port
                    new UsernamePasswordCredentials(defaultUserForDiscoveredNodes, defaultPasswordForDiscoveredNodes.toCharArray())
            );
        }
        return credentialsProvider;
    }
}
