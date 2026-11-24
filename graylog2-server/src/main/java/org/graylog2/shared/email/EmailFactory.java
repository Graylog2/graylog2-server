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

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.configuration.EmailConfiguration;

import java.util.function.Supplier;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Utility class to create preconfigured {@link Email} instances by applying the settings from
 * {@link EmailConfiguration}.
 */
@Singleton
public class EmailFactory {
    /**
     * JavaMail ships the SASL XOAUTH2 mechanism but doesn't enable it by default.
     */
    private static final String MAIL_SMTP_AUTH_MECHANISMS = "mail.smtp.auth.mechanisms";
    private static final String XOAUTH2 = "XOAUTH2";

    private final EmailConfiguration configuration;
    private final SmtpOAuth2TokenProvider oAuth2TokenProvider;

    @Inject
    public EmailFactory(EmailConfiguration configuration, SmtpOAuth2TokenProvider oAuth2TokenProvider) {
        this.configuration = configuration;
        this.oAuth2TokenProvider = oAuth2TokenProvider;
    }

    public boolean isEmailTransportEnabled() {
        return configuration.isEnabled();
    }

    /**
     * Creates a preconfigured {@link SimpleEmail} object with the settings from {@link EmailConfiguration} applied.
     *
     * @throws EmailException If applying configuration values fails.
     */
    public SimpleEmail simpleEmail() throws EmailException {
        return getPreconfigured(SimpleEmail::new);
    }

    /**
     * Creates a preconfigured {@link MultiPartEmail} object with the settings from {@link EmailConfiguration} applied.
     *
     * @throws EmailException If applying configuration values fails.
     */
    public MultiPartEmail multiPartEmail() throws EmailException {
        return getPreconfigured(MultiPartEmail::new);
    }

    /**
     * Creates a preconfigured {@link HtmlEmail} object with the settings from {@link EmailConfiguration} applied.
     *
     * @throws EmailException If applying configuration values fails.
     */
    public HtmlEmail htmlEmail() throws EmailException {
        return getPreconfigured(HtmlEmail::new);
    }

    /**
     * Creates a preconfigured {@link ImageHtmlEmail} object with the settings from {@link EmailConfiguration} applied.
     *
     * @throws EmailException If applying configuration values fails.
     */
    public ImageHtmlEmail imageHtmlEmail() throws EmailException {
        return getPreconfigured(ImageHtmlEmail::new);
    }

    /**
     * Creates a preconfigured {@link Email} object with the settings from {@link EmailConfiguration} applied.
     *
     * @param objectSupplier Supplier to create a raw object of the appropriate subtype of {@link Email}
     * @throws EmailException If applying configuration values fails.
     */
    public <T extends Email> T getPreconfigured(Supplier<T> objectSupplier) throws EmailException {
        return applyConfig(objectSupplier.get());
    }

    private <T extends Email> T applyConfig(T email) throws EmailException {
        email.setCharset(EmailConstants.UTF_8);
        email.setHostName(configuration.getHostname());
        email.setSmtpPort(configuration.getPort());

        if (configuration.isUseSsl()) {
            email.setSslSmtpPort(Integer.toString(configuration.getPort()));
        }

        if (configuration.isUseOAuth2()) {
            // With XOAUTH2, the access token takes the place of the password.
            email.setAuthenticator(new DefaultAuthenticator(
                    Strings.nullToEmpty(configuration.getUsername()),
                    oAuth2TokenProvider.getAccessToken()
            ));
        } else if (configuration.isUseAuth()) {
            email.setAuthenticator(new DefaultAuthenticator(
                    Strings.nullToEmpty(configuration.getUsername()),
                    Strings.nullToEmpty(configuration.getPassword())
            ));
        }

        email.setSSLOnConnect(configuration.isUseSsl());
        email.setStartTLSEnabled(configuration.isUseTls());

        if (!isNullOrEmpty(configuration.getFromEmail())) {
            email.setFrom(configuration.getFromEmail());
        }

        email.setSocketConnectionTimeout(Ints.saturatedCast(configuration.getSocketConnectionTimeout().toMillis()));
        email.setSocketTimeout(Ints.saturatedCast(configuration.getSocketTimeout().toMillis()));

        if (configuration.isUseOAuth2()) {
            // Creates the mail session from the settings above. The session properties are only consulted when
            // connecting, so restricting the mechanisms here affects this email only and no global state.
            email.getMailSession().getProperties().setProperty(MAIL_SMTP_AUTH_MECHANISMS, XOAUTH2);
        }

        return email;
    }
}
