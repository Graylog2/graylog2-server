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
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.google.common.base.Strings;
import org.graylog2.configuration.converters.JavaDurationConverter;

import java.net.URI;
import java.time.Duration;

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

    @Parameter(value = "transport_email_socket_connection_timeout",
               converter = JavaDurationConverter.class,
               validators = MillisecondDurationValidator.class)
    private Duration socketConnectionTimeout = Duration.ofSeconds(10);

    @Parameter(value = "transport_email_socket_timeout",
               converter = JavaDurationConverter.class,
               validators = MillisecondDurationValidator.class)
    private Duration socketTimeout = Duration.ofSeconds(10);

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
    public void validateTlsSsl() throws ValidationException {
        if (isUseTls() && isUseSsl()) {
            throw new ValidationException("SMTP over SSL (SMTPS) and SMTP with STARTTLS cannot be used at the same time.");
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateHostname() throws ValidationException {
        if (isEnabled() && Strings.isNullOrEmpty(getHostname())) {
            throw new ValidationException("No hostname configured for email transport");
        }
    }

    public Duration getSocketConnectionTimeout() {
        return socketConnectionTimeout;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public static class MillisecondDurationValidator implements Validator<Duration> {
        @Override
        public void validate(String name, Duration value) throws ValidationException {
            try {
                final long ignored = value.toNanos();
            } catch (ArithmeticException e) {
                throw new ValidationException("Parameter " + name +
                        " exceeds the limit to allow representation as milliseconds: " + e.getLocalizedMessage());
            }
        }
    }
}
