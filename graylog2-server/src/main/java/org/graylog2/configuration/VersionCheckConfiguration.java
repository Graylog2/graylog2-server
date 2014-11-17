/**
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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;

public class VersionCheckConfiguration {
    @Parameter(value = "versionchecks")
    private boolean enabled = true;

    @Parameter(value = "versionchecks_uri")
    private String uri = "http://versioncheck.torch.sh/check";

    @Parameter(value = "versionchecks_connect_timeout", validator = PositiveIntegerValidator.class)
    private int connectTimeOut = 10000;

    @Parameter(value = "versionchecks_socket_timeout", validator = PositiveIntegerValidator.class)
    private int socketTimeOut = 10000;

    @Parameter(value = "versionchecks_connection_request_timeout", validator = PositiveIntegerValidator.class)
    private int connectionRequestTimeOut = 10000;

    public boolean isEnabled() {
        return enabled;
    }

    public String getUri() {
        return uri;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public int getSocketTimeOut() {
        return socketTimeOut;
    }

    public int getConnectionRequestTimeOut() {
        return connectionRequestTimeOut;
    }
}
