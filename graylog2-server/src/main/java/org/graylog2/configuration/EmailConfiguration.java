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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.InetPortValidator;

import java.net.URI;

public class EmailConfiguration {
    @Parameter(value = "transport_email_enabled")
    private boolean enabled = false;

    @Parameter(value = "transport_email_hostname")
    private String hostname;

    @Parameter(value = "transport_email_port", validator = InetPortValidator.class)
    private int port = 25;

    @Parameter(value = "transport_email_use_auth")
    private boolean useAuth = false;

    @Parameter(value = "transport_email_use_tls")
    private boolean useTls = false;

    @Parameter(value = "transport_email_use_ssl")
    private boolean useSsl = true;

    @Parameter(value = "transport_email_auth_username")
    private String username;

    @Parameter(value = "transport_email_auth_password")
    private String password;

    @Parameter(value = "transport_email_from_email")
    private String fromEmail;

    @Parameter(value = "transport_email_web_interface_url")
    private URI webInterfaceUri;

    public boolean isEnabled() {
        return enabled;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public boolean isUseAuth() {
        return useAuth;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public URI getWebInterfaceUri() {
        return webInterfaceUri;
    }
}
