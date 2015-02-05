/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
package lib.security;

import org.apache.shiro.authc.HostAuthenticationToken;

public class SessionIdAuthenticationToken implements HostAuthenticationToken {

    private final String username;
    private final String sessionId;
    private final String host;

    public SessionIdAuthenticationToken(String username, String sessionId, String host) {
        this.username = username;
        this.sessionId = sessionId;
        this.host = host;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Object getPrincipal() {
        return sessionId;
    }

    @Override
    public Object getCredentials() {
        return "session";
    }

    public String getUsername() {
        return username;
    }
}
