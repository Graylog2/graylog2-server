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

import org.apache.shiro.authc.HostAuthenticationToken;

import javax.ws.rs.core.MultivaluedMap;

public class HttpHeadersToken implements HostAuthenticationToken {

    private final MultivaluedMap<String, String> httpHeaders;
    private final String host;

    public HttpHeadersToken(MultivaluedMap<String, String> httpHeaders, String host) {
        this.httpHeaders = httpHeaders;
        this.host = host;
    }

    @Override
    public Object getPrincipal() {
        return httpHeaders.get("remote_user");
    }

    @Override
    public Object getCredentials() {
        return httpHeaders.get("remote_credentials");
    }

    @Override
    public String getHost() {
        return host;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return httpHeaders;
    }
}
