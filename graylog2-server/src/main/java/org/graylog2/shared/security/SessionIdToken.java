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
package org.graylog2.shared.security;

import com.google.common.base.MoreObjects;
import org.apache.shiro.authc.HostAuthenticationToken;

import java.util.Objects;

public final class SessionIdToken implements HostAuthenticationToken {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionIdToken that = (SessionIdToken) o;
        return Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, host);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sessionId", sessionId)
                .add("host", host)
                .toString();
    }
}
