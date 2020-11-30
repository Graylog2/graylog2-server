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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
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
    private boolean useTls = true;

    @Parameter(value = "transport_email_use_ssl")
    private boolean useSsl = false;

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

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateConfig() throws ValidationException {
        if (isUseTls() && isUseSsl()) {
            throw new ValidationException("SMTP over SSL (SMTPS) and SMTP with STARTTLS cannot be used at the same time.");
        }
    }
}
