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
package org.graylog2.shared.security;

import org.apache.shiro.authc.HostAuthenticationToken;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

public class HttpHeadersToken implements HostAuthenticationToken {

    private final MultivaluedMap<String, String> httpHeaders;
    private final String host;
    private final String remoteAddr;

    public HttpHeadersToken(MultivaluedMap<String, String> httpHeaders, String host, String remoteAddr) {
        this.httpHeaders = httpHeaders;
        this.host = host;
        this.remoteAddr = remoteAddr;
    }

    /**
     * A HttpHeadersToken does not have a natural principal associated with it, so this is always null.
     *
     * @return null
     */
    @Override
    @Nullable
    public Object getPrincipal() {
        return null;
    }

    /**
     * A HttpHeadersToken does not have a natural credential associated with it, so this is always null.
     *
     * @return null
     */
    @Override
    @Nullable
    public Object getCredentials() {
        return null;
    }

    @Override
    public String getHost() {
        return host;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return httpHeaders;
    }

    /**
     * The direct remote address, if the request came through a proxy, this will be the address of last hop.
     * Typically used to verify that a client is "trusted".
     * @return the direct peer's address
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }
}
