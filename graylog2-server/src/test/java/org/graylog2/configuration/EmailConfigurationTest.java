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
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EmailConfigurationTest {
    @Test
    public void validationSucceedsIfSSLAndTLSAreDisabled() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> config = ImmutableMap.of(
                "transport_email_hostname", "localhost",
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
                "transport_email_hostname", "localhost",
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
                "transport_email_hostname", "localhost",
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
                "transport_email_hostname", "localhost",
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

    @Test
    public void validateTimeouts() throws ValidationException, RepositoryException {
        assertThatThrownBy(toConfig("transport_email_socket_connection_timeout", Long.MAX_VALUE + " seconds")::process)
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds the limit");

        assertThatThrownBy(toConfig("transport_email_socket_timeout", Long.MAX_VALUE + " seconds")::process)
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds the limit");

        toConfig("transport_email_socket_connection_timeout", "120 seconds").process();
        toConfig("transport_email_socket_timeout", "120 seconds").process();
    }

    @Test
    public void authMethodDefaultsToPassword() throws ValidationException, RepositoryException {
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        new JadConfig(new InMemoryRepository(Map.of("transport_email_use_auth", "true")), emailConfiguration).process();

        assertThat(emailConfiguration.getAuthMethod()).isEqualTo(EmailConfiguration.AuthMethod.PASSWORD);
        assertThat(emailConfiguration.isUseOAuth2()).isFalse();
    }

    @Test
    public void parsesOAuth2Settings() throws ValidationException, RepositoryException {
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        new JadConfig(new InMemoryRepository(oauth2Settings()), emailConfiguration).process();

        assertThat(emailConfiguration.getAuthMethod()).isEqualTo(EmailConfiguration.AuthMethod.OAUTH2);
        assertThat(emailConfiguration.isUseOAuth2()).isTrue();
        assertThat(emailConfiguration.getOAuth2TokenUrl()).isEqualTo(URI.create("https://login.microsoftonline.com/tenant/oauth2/v2.0/token"));
        assertThat(emailConfiguration.getOAuth2ClientId()).isEqualTo("client-id");
        assertThat(emailConfiguration.getOAuth2ClientSecret()).isEqualTo("client-secret");
        assertThat(emailConfiguration.getOAuth2Scope()).isEqualTo("https://outlook.office365.com/.default");
    }

    @Test
    public void validationFailsIfOAuth2IsUsedWithoutUseAuth() {
        final Map<String, String> settings = oauth2Settings();
        settings.put("transport_email_use_auth", "false");

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ValidationException.class)
                .hasMessage("transport_email_auth_method = oauth2 requires transport_email_use_auth = true");
    }

    @Test
    public void validationFailsIfOAuth2SettingsAreMissing() {
        final Map<String, String> settings = oauth2Settings();
        settings.remove("transport_email_auth_username");
        settings.remove("transport_email_auth_oauth2_client_secret");
        settings.remove("transport_email_auth_oauth2_scope");

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ValidationException.class)
                .hasMessage("OAuth 2.0 email authentication requires the following settings: transport_email_auth_username, " +
                        "transport_email_auth_oauth2_client_secret (or transport_email_auth_oauth2_client_certificate_file " +
                        "and transport_email_auth_oauth2_client_private_key_file), transport_email_auth_oauth2_scope");
    }

    @Test
    public void parsesOAuth2ClientCertificateSettings(@TempDir Path tempDir) throws Exception {
        final Map<String, String> settings = oauth2CertificateSettings(tempDir);
        settings.put("transport_email_auth_oauth2_client_private_key_password", "key-password");
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        new JadConfig(new InMemoryRepository(settings), emailConfiguration).process();

        assertThat(emailConfiguration.isUseOAuth2ClientCertificate()).isTrue();
        assertThat(emailConfiguration.getOAuth2ClientCertificateFile()).isEqualTo(tempDir.resolve("client.crt"));
        assertThat(emailConfiguration.getOAuth2ClientPrivateKeyFile()).isEqualTo(tempDir.resolve("client.key"));
        assertThat(emailConfiguration.getOAuth2ClientPrivateKeyPassword()).isEqualTo("key-password");
    }

    @Test
    public void clientSecretDoesNotUseCertificate() throws Exception {
        final EmailConfiguration emailConfiguration = new EmailConfiguration();
        new JadConfig(new InMemoryRepository(oauth2Settings()), emailConfiguration).process();

        assertThat(emailConfiguration.isUseOAuth2ClientCertificate()).isFalse();
    }

    @Test
    public void validationFailsIfClientSecretAndCertificateAreConfigured(@TempDir Path tempDir) throws Exception {
        final Map<String, String> settings = oauth2CertificateSettings(tempDir);
        settings.put("transport_email_auth_oauth2_client_secret", "client-secret");

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ValidationException.class)
                .hasMessage("Configure either transport_email_auth_oauth2_client_secret or " +
                        "transport_email_auth_oauth2_client_certificate_file and transport_email_auth_oauth2_client_private_key_file, not both");
    }

    @Test
    public void validationFailsIfCertificateIsConfiguredWithoutPrivateKey(@TempDir Path tempDir) throws Exception {
        final Map<String, String> settings = oauth2CertificateSettings(tempDir);
        settings.remove("transport_email_auth_oauth2_client_private_key_file");

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ValidationException.class)
                .hasMessage("OAuth 2.0 email authentication requires the following settings: " +
                        "transport_email_auth_oauth2_client_private_key_file");
    }

    @Test
    public void validationFailsIfPrivateKeyIsConfiguredWithoutCertificate(@TempDir Path tempDir) throws Exception {
        final Map<String, String> settings = oauth2CertificateSettings(tempDir);
        settings.remove("transport_email_auth_oauth2_client_certificate_file");

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ValidationException.class)
                .hasMessage("OAuth 2.0 email authentication requires the following settings: " +
                        "transport_email_auth_oauth2_client_certificate_file");
    }

    @Test
    public void validationFailsIfCertificateFileIsNotReadable(@TempDir Path tempDir) throws Exception {
        final Map<String, String> settings = oauth2CertificateSettings(tempDir);
        settings.put("transport_email_auth_oauth2_client_certificate_file", tempDir.resolve("missing.crt").toString());

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("missing.crt");
    }

    @Test
    public void validationSkipsOAuth2SettingsIfTransportIsDisabled() throws ValidationException, RepositoryException {
        final Map<String, String> settings = new HashMap<>(Map.of(
                "transport_email_use_auth", "true",
                "transport_email_auth_method", "oauth2"
        ));
        new JadConfig(new InMemoryRepository(settings), new EmailConfiguration()).process();
    }

    @Test
    public void validationFailsForNonHttpTokenUrl() {
        final Map<String, String> settings = oauth2Settings();
        settings.put("transport_email_auth_oauth2_token_url", "file:///etc/passwd");

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ValidationException.class)
                .hasMessage("transport_email_auth_oauth2_token_url must be an http(s) URL");
    }

    @Test
    public void parsingFailsForUnknownAuthMethod() {
        final Map<String, String> settings = oauth2Settings();
        settings.put("transport_email_auth_method", "kerberos");

        assertThatThrownBy(new JadConfig(new InMemoryRepository(settings), new EmailConfiguration())::process)
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("transport_email_auth_method")
                .rootCause()
                .hasMessage("Unable to parse email authentication method <kerberos>. Valid methods are: [password, oauth2].");
    }

    private Map<String, String> oauth2Settings() {
        return new HashMap<>(Map.of(
                "transport_email_enabled", "true",
                "transport_email_hostname", "smtp.office365.com",
                "transport_email_use_auth", "true",
                "transport_email_auth_method", "OAuth2",
                "transport_email_auth_username", "graylog@example.com",
                "transport_email_auth_oauth2_token_url", "https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
                "transport_email_auth_oauth2_client_id", "client-id",
                "transport_email_auth_oauth2_client_secret", "client-secret",
                "transport_email_auth_oauth2_scope", "https://outlook.office365.com/.default"
        ));
    }

    private Map<String, String> oauth2CertificateSettings(Path tempDir) throws IOException {
        final Map<String, String> settings = oauth2Settings();
        settings.remove("transport_email_auth_oauth2_client_secret");
        settings.put("transport_email_auth_oauth2_client_certificate_file",
                Files.writeString(tempDir.resolve("client.crt"), "certificate").toString());
        settings.put("transport_email_auth_oauth2_client_private_key_file",
                Files.writeString(tempDir.resolve("client.key"), "key").toString());
        return settings;
    }

    private JadConfig toConfig(String key, String value) {
        return new JadConfig(new InMemoryRepository(Map.of(key, value)), new EmailConfiguration());
    }
}
