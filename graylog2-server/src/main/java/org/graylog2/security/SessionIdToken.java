/*
 * Copyright 2013 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

import org.apache.shiro.authc.HostAuthenticationToken;

public class SessionIdToken implements HostAuthenticationToken {

    private final String sessionId;
    private final String host;

    public SessionIdToken(String sessionId, String host) {
        this.sessionId = sessionId;
        this.host = host;
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

    @Override
    public String getHost() {
        return host;
    }


}
