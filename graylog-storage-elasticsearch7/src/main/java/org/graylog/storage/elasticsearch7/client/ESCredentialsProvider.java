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
package org.graylog.storage.elasticsearch7.client;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.graylog.shaded.elasticsearch7.org.apache.http.auth.AuthScope;
import org.graylog.shaded.elasticsearch7.org.apache.http.auth.UsernamePasswordCredentials;
import org.graylog.shaded.elasticsearch7.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.elasticsearch7.org.apache.http.impl.client.BasicCredentialsProvider;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

public class ESCredentialsProvider implements Provider<CredentialsProvider> {
    private final List<URI> elasticsearchHosts;
    private final String defaultUserForDiscoveredNodes;
    private final String defaultPasswordForDiscoveredNodes;
    private final boolean discoveryEnabled;

    @Inject
    public ESCredentialsProvider(@Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
                                 @Named("elasticsearch_discovery_default_user") @Nullable String defaultUserForDiscoveredNodes,
                                 @Named("elasticsearch_discovery_default_password") @Nullable String defaultPasswordForDiscoveredNodes,
                                 @Named("elasticsearch_discovery_enabled") boolean discoveryEnabled) {
        this.elasticsearchHosts = elasticsearchHosts;
        this.defaultUserForDiscoveredNodes = defaultUserForDiscoveredNodes;
        this.defaultPasswordForDiscoveredNodes = defaultPasswordForDiscoveredNodes;
        this.discoveryEnabled = discoveryEnabled;
    }

    @Override
    public CredentialsProvider get() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        elasticsearchHosts
                .forEach(hostUri -> {
                    if (!Strings.isNullOrEmpty(hostUri.getUserInfo())) {
                        final Iterator<String> splittedUserInfo = Splitter.on(":")
                                .split(hostUri.getUserInfo())
                                .iterator();
                        if (splittedUserInfo.hasNext()) {
                            final String username = splittedUserInfo.next();
                            final String password = splittedUserInfo.hasNext() ? splittedUserInfo.next() : null;
                            credentialsProvider.setCredentials(
                                    new AuthScope(hostUri.getHost(), hostUri.getPort(), AuthScope.ANY_REALM, AuthScope.ANY_SCHEME),
                                    new UsernamePasswordCredentials(username, password)
                            );
                        }
                    }
                });

        if (discoveryEnabled && !Strings.isNullOrEmpty(defaultUserForDiscoveredNodes) && !Strings.isNullOrEmpty(defaultPasswordForDiscoveredNodes)) {
            credentialsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME),
                    new UsernamePasswordCredentials(defaultUserForDiscoveredNodes, defaultPasswordForDiscoveredNodes)
            );
        }

        return credentialsProvider;
    }
}
