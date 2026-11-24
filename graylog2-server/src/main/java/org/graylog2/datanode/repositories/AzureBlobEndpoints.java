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
package org.graylog2.datanode.repositories;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * Azure blob endpoints that an OpenSearch Azure repository client can address. OpenSearch builds the endpoint from
 * the account name and the {@code endpoint_suffix} setting, always as {@code https://<account>.blob.<suffix>}, so
 * other endpoints, e.g. emulators, plain http or private endpoints with a custom host name, can't be used.
 */
public final class AzureBlobEndpoints {

    private static final int HTTPS_PORT = 443;

    private AzureBlobEndpoints() {
    }

    /**
     * The {@code endpoint_suffix} of the given endpoint of a storage account, e.g. {@code core.chinacloudapi.cn} for
     * {@code https://account.blob.core.chinacloudapi.cn}. Empty if OpenSearch can't address the endpoint.
     */
    public static Optional<String> endpointSuffix(String accountName, URI endpoint) {
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null
                || (endpoint.getPort() != -1 && endpoint.getPort() != HTTPS_PORT)
                || !(StringUtils.isEmpty(endpoint.getRawPath()) || "/".equals(endpoint.getRawPath()))) {
            return Optional.empty();
        }
        final String host = endpoint.getHost().toLowerCase(Locale.ROOT);
        final String prefix = accountName.toLowerCase(Locale.ROOT) + ".blob.";
        return host.startsWith(prefix) && host.length() > prefix.length()
                ? Optional.of(host.substring(prefix.length()))
                : Optional.empty();
    }
}
