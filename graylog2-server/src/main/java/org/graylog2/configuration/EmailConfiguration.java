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
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.google.common.base.Strings;
import org.graylog2.configuration.converters.JavaDurationConverter;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.graylog2.shared.utilities.StringUtils.f;

@DocumentationSection(heading = "Email transport", description = "")
public class EmailConfiguration {
    @Documentation("Enable sending emails (e. g. for email notifications).")
    @Parameter(value = "transport_email_enabled")
    private boolean enabled = false;

    @Documentation("Hostname of the SMTP server used to send emails.")
    @Parameter(value = "transport_email_hostname")
    private String hostname;

    @Documentation("Port of the SMTP server used to send emails.")
    @Parameter(value = "transport_email_port", validator = InetPortValidator.class)
    private int port = 25;

    @Documentation("""
            Authenticate against the SMTP server. The authentication method is selected with transport_email_auth_method.
            """)
    @Parameter(value = "transport_email_use_auth")
    private boolean useAuth = false;

    @Documentation("""
            Authentication method used when transport_email_use_auth is enabled.

              password: Authenticate with transport_email_auth_username and transport_email_auth_password.
              oauth2:   Authenticate with an OAuth 2.0 access token (SASL XOAUTH2), obtained with the client credentials
                        grant using the transport_email_auth_oauth2_* settings. Requires transport_email_use_auth = true.
                        transport_email_auth_username must be set to the mailbox used for sending. Use this for Microsoft
                        Exchange Online / Microsoft 365, which no longer accepts basic (password) authentication for SMTP.
            """)
    @Parameter(value = "transport_email_auth_method", converter = AuthMethod.Converter.class)
    private AuthMethod authMethod = AuthMethod.PASSWORD;

    @Documentation("""
            OAuth 2.0 token endpoint used to obtain access tokens for SMTP authentication.
            Microsoft 365: https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token
            """)
    @Parameter(value = "transport_email_auth_oauth2_token_url")
    private URI oauth2TokenUrl;

    @Documentation("OAuth 2.0 client ID used to obtain access tokens for SMTP authentication.")
    @Parameter(value = "transport_email_auth_oauth2_client_id")
    private String oauth2ClientId;

    @Documentation("OAuth 2.0 client secret used to obtain access tokens for SMTP authentication.")
    @Parameter(value = "transport_email_auth_oauth2_client_secret")
    private String oauth2ClientSecret;

    @Documentation("""
            OAuth 2.0 scope requested for SMTP access tokens. Multiple scopes are separated by spaces.
            Microsoft 365: https://outlook.office365.com/.default
            """)
    @Parameter(value = "transport_email_auth_oauth2_scope")
    private String oauth2Scope;

    @Documentation("""
            Encryption settings

            ATTENTION:
               Using SMTP with STARTTLS *and* SMTPS at the same time is *not* possible.

            Use SMTP with STARTTLS, see https://en.wikipedia.org/wiki/Opportunistic_TLS
            """)
    @Parameter(value = "transport_email_use_tls")
    private boolean useTls = true;

    @Documentation("""
            Use SMTP over SSL (SMTPS), see https://en.wikipedia.org/wiki/SMTPS
            This is deprecated on most SMTP services!
            """)
    @Parameter(value = "transport_email_use_ssl")
    private boolean useSsl = false;

    @Documentation("Username for authenticating against the SMTP server.")
    @Parameter(value = "transport_email_auth_username")
    private String username;

    @Documentation("Password for authenticating against the SMTP server.")
    @Parameter(value = "transport_email_auth_password")
    private String password;

    @Documentation("Default sender (From) email address for emails sent by Graylog.")
    @Parameter(value = "transport_email_from_email")
    private String fromEmail;

    @Documentation("""
            Specify and uncomment this if you want to include links to the stream in your stream alert mails.
            This should define the fully qualified base url to your web interface exactly the same way as it is accessed by your users.
            """)
    @Parameter(value = "transport_email_web_interface_url")
    private URI webInterfaceUri;

    @Documentation("Timeout for establishing the connection to the SMTP server.")
    @Parameter(value = "transport_email_socket_connection_timeout",
               converter = JavaDurationConverter.class,
               validators = MillisecondDurationValidator.class)
    private Duration socketConnectionTimeout = Duration.ofSeconds(10);

    @Documentation("Socket read timeout for the connection to the SMTP server.")
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

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    public boolean isUseOAuth2() {
        return isUseAuth() && authMethod == AuthMethod.OAUTH2;
    }

    public URI getOAuth2TokenUrl() {
        return oauth2TokenUrl;
    }

    public String getOAuth2ClientId() {
        return oauth2ClientId;
    }

    public String getOAuth2ClientSecret() {
        return oauth2ClientSecret;
    }

    public String getOAuth2Scope() {
        return oauth2Scope;
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

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateOAuth2() throws ValidationException {
        if (!isEnabled() || authMethod != AuthMethod.OAUTH2) {
            return;
        }
        if (!isUseAuth()) {
            throw new ValidationException("transport_email_auth_method = oauth2 requires transport_email_use_auth = true");
        }
        final List<String> missing = new ArrayList<>();
        if (Strings.isNullOrEmpty(getUsername())) {
            missing.add("transport_email_auth_username");
        }
        if (getOAuth2TokenUrl() == null) {
            missing.add("transport_email_auth_oauth2_token_url");
        }
        if (Strings.isNullOrEmpty(getOAuth2ClientId())) {
            missing.add("transport_email_auth_oauth2_client_id");
        }
        if (Strings.isNullOrEmpty(getOAuth2ClientSecret())) {
            missing.add("transport_email_auth_oauth2_client_secret");
        }
        if (Strings.isNullOrEmpty(getOAuth2Scope())) {
            missing.add("transport_email_auth_oauth2_scope");
        }
        if (!missing.isEmpty()) {
            throw new ValidationException(f("OAuth 2.0 email authentication requires the following settings: %s",
                    String.join(", ", missing)));
        }
        final String scheme = getOAuth2TokenUrl().getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new ValidationException("transport_email_auth_oauth2_token_url must be an http(s) URL");
        }
    }

    public Duration getSocketConnectionTimeout() {
        return socketConnectionTimeout;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public enum AuthMethod {
        PASSWORD,
        OAUTH2;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static class Converter implements com.github.joschi.jadconfig.Converter<AuthMethod> {
            @Override
            public AuthMethod convertFrom(String value) {
                try {
                    return AuthMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new ParameterException(f("Unable to parse email authentication method <%s>. Valid methods are: %s.",
                            value, Arrays.toString(AuthMethod.values())));
                }
            }

            @Override
            public String convertTo(AuthMethod value) {
                return value.toString();
            }
        }
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
