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

package org.graylog2.shared.rest.resources;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;

public abstract class ProxiedResource {
    protected final String authenticationToken;

    public ProxiedResource(@Context HttpHeaders httpHeaders) {
        final List<String> authenticationTokens = httpHeaders.getRequestHeader("Authorization");
        if (authenticationTokens != null && authenticationTokens.size() >= 1) {
            this.authenticationToken = authenticationTokens.get(0);
        } else {
            this.authenticationToken = null;
        }
    }
}
