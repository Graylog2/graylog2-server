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

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EmailConfigurationTest {
    @Test
    public void validationSucceedsIfSSLAndTLSAreDisabled() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> config = ImmutableMap.of(
                "transport_email_enabled", "true",
                "transport_email_use_tls", "false",
                "transport_email_use_ssl", "false"
        );
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        final JadConfig jadConfig = new JadConfig(new InMemoryRepository(config), emailConfiguration);
        jadConfig.process();

        assertThat(emailConfiguration.isUseSsl()).isFalse();
        assertThat(emailConfiguration.isUseTls()).isFalse();
    }

    @Test
    public void validationSucceedsIfSSLIsEnabledAndTLSIsDisabled() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> config = ImmutableMap.of(
                "transport_email_enabled", "true",
                "transport_email_use_tls", "false",
                "transport_email_use_ssl", "true"
        );
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        final JadConfig jadConfig = new JadConfig(new InMemoryRepository(config), emailConfiguration);
        jadConfig.process();

        assertThat(emailConfiguration.isUseSsl()).isTrue();
        assertThat(emailConfiguration.isUseTls()).isFalse();
    }

    @Test
    public void validationSucceedsIfSSLIsDisabledAndTLSIsEnabled() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> config = ImmutableMap.of(
                "transport_email_enabled", "true",
                "transport_email_use_tls", "true",
                "transport_email_use_ssl", "false"
        );
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        final JadConfig jadConfig = new JadConfig(new InMemoryRepository(config), emailConfiguration);
        jadConfig.process();

        assertThat(emailConfiguration.isUseSsl()).isFalse();
        assertThat(emailConfiguration.isUseTls()).isTrue();
    }

    @Test
    public void validationFailsIfSSLandTLSAreBothEnabled() {
        final ImmutableMap<String, String> config = ImmutableMap.of(
                "transport_email_enabled", "true",
                "transport_email_use_tls", "true",
                "transport_email_use_ssl", "true"
        );
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        final JadConfig jadConfig = new JadConfig(new InMemoryRepository(config), emailConfiguration);

        assertThatThrownBy(jadConfig::process)
                .isInstanceOf(ValidationException.class)
                .hasMessage("SMTP over SSL (SMTPS) and SMTP with STARTTLS cannot be used at the same time.");
    }
}