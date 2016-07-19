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

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

public class HttpHeadersToken implements HostAuthenticationToken {

    private final MultivaluedMap<String, String> httpHeaders;
    private final String host;

    public HttpHeadersToken(MultivaluedMap<String, String> httpHeaders, String host) {
        this.httpHeaders = httpHeaders;
        this.host = host;
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
}
