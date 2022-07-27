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
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.configuration.EmailConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Supplier;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Utility class to create preconfigured {@link Email} instances by applying the settings from
 * {@link EmailConfiguration}.
 */
@Singleton
public class EmailFactory {
    private final EmailConfiguration configuration;

    @Inject
    public EmailFactory(EmailConfiguration configuration) {
        this.configuration = configuration;
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

        if (configuration.isUseAuth()) {
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

        return email;
    }
}
