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

import com.google.common.base.MoreObjects;
import org.apache.shiro.authc.HostAuthenticationToken;

import java.util.Objects;

public final class SessionIdToken implements HostAuthenticationToken, RemoteAddressAuthenticationToken {

    private final String sessionId;
    private final String host;
    private final String remoteAddr;

    public SessionIdToken(String sessionId, String host, String remoteAddr) {
        this.sessionId = sessionId;
        this.host = host;
        this.remoteAddr = remoteAddr;
    }

    @Override
    public Object getPrincipal() {
        return sessionId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionIdToken that = (SessionIdToken) o;
        return Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(host, that.host) &&
                Objects.equals(remoteAddr, that.remoteAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, host, remoteAddr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hashcode", hashCode())
                .add("host", host)
                .toString();
    }
}
