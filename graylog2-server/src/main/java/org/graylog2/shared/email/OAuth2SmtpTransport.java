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
package org.graylog2.shared.email;

import com.sun.mail.smtp.SMTPTransport;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.URLName;

import java.util.Properties;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * SMTP transport that authenticates with an OAuth 2.0 access token (SASL XOAUTH2).
 * <p>
 * The token is requested when the connection is established rather than when the email is created, so it is as fresh
 * as possible. If the SMTP server rejects a cached token, the token is invalidated and authentication is retried once
 * with a newly requested token.
 * <p>
 * JavaMail instantiates transports reflectively, so the token provider is passed in through the session properties.
 * Use {@link #configure(Session, SmtpOAuth2TokenProvider, String)} to set up a session.
 */
public class OAuth2SmtpTransport extends SMTPTransport {
    private static final Logger LOG = LoggerFactory.getLogger(OAuth2SmtpTransport.class);

    private static final String TOKEN_PROVIDER_PROPERTY = OAuth2SmtpTransport.class.getName() + ".tokenProvider";

    public OAuth2SmtpTransport(Session session, URLName urlname) {
        super(session, urlname);
    }

    /**
     * Configures the given session to send emails with this transport, authenticating as {@code username}.
     * Only the given session is affected.
     */
    public static void configure(Session session, SmtpOAuth2TokenProvider tokenProvider, String username) {
        final Properties properties = session.getProperties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.user", username);
        // JavaMail ships the XOAUTH2 mechanism, but doesn't enable it by default.
        properties.setProperty("mail.smtp.auth.mechanisms", "XOAUTH2");
        properties.put(TOKEN_PROVIDER_PROPERTY, tokenProvider);
        try {
            session.setProvider(new Provider(Provider.Type.TRANSPORT, "smtp", OAuth2SmtpTransport.class.getName(),
                    "Graylog", null));
        } catch (MessagingException e) {
            // Only thrown for a null provider
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected synchronized boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
        final SmtpOAuth2TokenProvider tokenProvider = tokenProvider();
        if (tokenProvider == null || user == null) {
            return super.protocolConnect(host, port, user, password);
        }

        final SmtpOAuth2TokenProvider.AccessToken token = tokenProvider.getAccessToken();
        AuthenticationFailedException failure = connectWithToken(host, port, user, token.value());
        if (failure == null) {
            return true;
        }

        // A freshly requested token being rejected points to a configuration problem, so only retry cached tokens.
        if (token.fromCache()) {
            LOG.info("SMTP server <{}> rejected cached OAuth 2.0 access token, retrying with a new token", host);
            tokenProvider.invalidate(token.value());
            failure = connectWithToken(host, port, user, tokenProvider.getAccessToken().value());
            if (failure == null) {
                return true;
            }
        }

        LOG.warn(failure.getMessage());
        // Throw instead of returning false, because JavaMail would otherwise retry without credentials.
        throw failure;
    }

    /**
     * @return {@code null} if the connection was established, the authentication failure otherwise
     */
    @Nullable
    private AuthenticationFailedException connectWithToken(String host, int port, String user, String token) throws MessagingException {
        try {
            if (super.protocolConnect(host, port, user, token)) {
                return null;
            }
            return new AuthenticationFailedException(authFailureMessage(host, user));
        } catch (AuthenticationFailedException e) {
            return new AuthenticationFailedException(authFailureMessage(host, user), e);
        }
    }

    private String authFailureMessage(String host, String user) {
        final String serverResponse = getLastServerResponse();
        return f("SMTP server <%s> rejected the OAuth 2.0 access token for <%s>%s", host, user,
                serverResponse == null || serverResponse.isBlank() ? "" : f(": %s", serverResponse.trim()));
    }

    @Nullable
    private SmtpOAuth2TokenProvider tokenProvider() {
        return session.getProperties().get(TOKEN_PROVIDER_PROPERTY) instanceof SmtpOAuth2TokenProvider provider ? provider : null;
    }
}
