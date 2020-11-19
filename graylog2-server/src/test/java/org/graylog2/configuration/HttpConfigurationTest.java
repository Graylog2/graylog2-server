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
import com.github.joschi.jadconfig.guava.GuavaConverterFactory;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpConfigurationTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private HttpConfiguration configuration;
    private JadConfig jadConfig;

    @Before
    public void setUp() {
        configuration = new HttpConfiguration();
        jadConfig = new JadConfig().addConverterFactory(new GuavaConverterFactory());
    }

    @Test
    public void testHttpBindAddressIsIPv6Address() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "[2001:db8::1]:9000")))
                .addConfigurationBean(configuration)
                .process();

        assertThat(configuration.getHttpBindAddress()).isEqualTo(HostAndPort.fromParts("[2001:db8::1]", 9000));
    }

    @Test
    @Ignore("Disabled test due to being unreliable (see https://github.com/Graylog2/graylog2-server/issues/4459)")
    public void testHttpBindAddressIsIPv6AddressWithoutBrackets() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Possible bracketless IPv6 literal: 2001:db8::1");

        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "2001:db8::1")))
                .addConfigurationBean(configuration)
                .process();
    }

    @Test
    @Ignore("Disabled test due to being unreliable (see https://github.com/Graylog2/graylog2-server/issues/4459)")
    public void testHttpBindAddressIsInvalidIPv6Address() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Possible bracketless IPv6 literal: ff$$::1");

        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "ff$$::1")))
                .addConfigurationBean(configuration)
                .process();
    }

    @Test
    public void testHttpBindAddressIsIPv4Address() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "10.2.3.4:9000")))
                .addConfigurationBean(configuration)
                .process();

        assertThat(configuration.getHttpBindAddress()).isEqualTo(HostAndPort.fromParts("10.2.3.4", 9000));
    }

    @Test
    @Ignore("Disabled test due to being unreliable (see https://github.com/Graylog2/graylog2-server/issues/4459)")
    public void testHttpBindAddressIsInvalidIPv4Address() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("1234.5.6.7: ");

        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "1234.5.6.7:9000")))
                .addConfigurationBean(configuration)
                .process();
    }

    @Test
    @Ignore("Disabled test due to being unreliable (see https://github.com/Graylog2/graylog2-server/issues/4459)")
    public void testHttpBindAddressIsInvalidHostName() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("this-does-not-exist-42: ");

        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "this-does-not-exist-42")))
                .addConfigurationBean(configuration)
                .process();
    }

    @Test
    public void testHttpBindAddressIsValidHostname() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "example.com:9000")))
                .addConfigurationBean(configuration)
                .process();

        assertThat(configuration.getHttpBindAddress()).isEqualTo(HostAndPort.fromParts("example.com", 9000));
    }

    @Test
    public void testHttpBindAddressWithDefaultPort() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "example.com")))
                .addConfigurationBean(configuration)
                .process();

        assertThat(configuration.getHttpBindAddress()).isEqualTo(HostAndPort.fromParts("example.com", 9000));
    }

    @Test
    public void testHttpBindAddressWithCustomPort() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "example.com:12345")))
                .addConfigurationBean(configuration)
                .process();

        assertThat(configuration.getHttpBindAddress()).isEqualTo(HostAndPort.fromParts("example.com", 12345));
    }

    @Test
    public void testHttpPublishUriLocalhost() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "127.0.0.1:9000"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getDefaultHttpUri()).isEqualTo(URI.create("http://127.0.0.1:9000/"));
    }

    @Test
    public void testHttpBindAddressWildcard() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "0.0.0.0:9000"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getDefaultHttpUri())
                .isNotNull()
                .isNotEqualTo(URI.create("http://0.0.0.0:9000"));
    }

    @Test
    public void testHttpBindAddressIPv6Wildcard() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "[::]:9000"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getDefaultHttpUri())
                .isNotNull()
                .isNotEqualTo(URI.create("http://[::]:9000"));
    }

    @Test
    public void testHttpPublishUriWildcard() throws RepositoryException, ValidationException {
        final Map<String, String> properties = ImmutableMap.of(
                "http_bind_address", "0.0.0.0:9000",
                "http_publish_uri", "http://0.0.0.0:9000/");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri()).isNotEqualTo(URI.create("http://0.0.0.0:9000/"));
    }

    @Test
    public void testHttpPublishUriIPv6Wildcard() throws RepositoryException, ValidationException {
        final Map<String, String> properties = ImmutableMap.of(
                "http_bind_address", "[::]:9000",
                "http_publish_uri", "http://[::]:9000/");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri()).isNotEqualTo(URI.create("http://[::]:9000/"));
    }

    @Test
    public void testHttpPublishUriWildcardKeepsPath() throws RepositoryException, ValidationException {
        final Map<String, String> properties = ImmutableMap.of(
                "http_bind_address", "0.0.0.0:9000",
                "http_publish_uri", "http://0.0.0.0:9000/api/");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri())
                .hasPath("/api/")
                .isNotEqualTo(URI.create("http://0.0.0.0:9000/api/"));
    }

    @Test
    public void testHttpPublishUriIPv6WildcardKeepsPath() throws RepositoryException, ValidationException {
        final Map<String, String> properties = ImmutableMap.of(
                "http_bind_address", "[::]:9000",
                "http_publish_uri", "http://[::]:9000/api/");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri())
                .hasPath("/api/")
                .isNotEqualTo(URI.create("http://[::]:9000/api/"));
    }

    @Test
    public void testHttpPublishUriCustomAddress() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_bind_address", "10.0.0.1:9000"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getDefaultHttpUri().toString()).isEqualTo("http://10.0.0.1:9000/");
    }

    @Test
    public void testGetUriScheme() throws RepositoryException, ValidationException, IOException {
        final HttpConfiguration configWithoutTls = new HttpConfiguration();
        new JadConfig(new InMemoryRepository(ImmutableMap.of("http_enable_tls", "false")), configWithoutTls)
                .addConverterFactory(new GuavaConverterFactory())
                .process();
        assertThat(configWithoutTls.getUriScheme()).isEqualTo("http");

        final Map<String, String> properties = ImmutableMap.of(
                "http_bind_address", "127.0.0.1:9000",
                "http_enable_tls", "true",
                "http_tls_key_file", temporaryFolder.newFile("graylog.key").getAbsolutePath(),
                "http_tls_cert_file", temporaryFolder.newFile("graylog.crt").getAbsolutePath());
        final HttpConfiguration configWithTls = new HttpConfiguration();
        new JadConfig(new InMemoryRepository(properties), configWithTls)
                .addConverterFactory(new GuavaConverterFactory())
                .process();
        assertThat(configWithTls.getUriScheme()).isEqualTo("https");
    }

    @Test
    public void tlsValidationFailsIfPrivateKeyIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final Map<String, String> properties = ImmutableMap.of(
                "http_enable_tls", "true",
                "http_tls_key_file", privateKey.getAbsolutePath(),
                "http_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing HTTP private key: ");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();
    }

    @Test
    public void tlsValidationFailsIfPrivateKeyIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFolder("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final Map<String, String> properties = ImmutableMap.of(
                "http_enable_tls", "true",
                "http_tls_key_file", privateKey.getAbsolutePath(),
                "http_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing HTTP private key: ");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();
    }

    @Test
    public void tlsValidationFailsIfPrivateKeyIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final Map<String, String> properties = ImmutableMap.of(
                "http_enable_tls", "true",
                "http_tls_key_file", privateKey.getAbsolutePath(),
                "http_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing HTTP private key: ");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();
    }

    @Test
    public void tlsValidationFailsIfCertificateIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final Map<String, String> properties = ImmutableMap.of(
                "http_enable_tls", "true",
                "http_tls_key_file", privateKey.getAbsolutePath(),
                "http_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing HTTP X.509 certificate: ");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();
    }

    @Test
    public void tlsValidationFailsIfCertificateIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFolder("graylog.crt");

        final Map<String, String> properties = ImmutableMap.of(
                "http_enable_tls", "true",
                "http_tls_key_file", privateKey.getAbsolutePath(),
                "http_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing HTTP X.509 certificate: ");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();
    }

    @Test
    public void tlsValidationFailsIfCertificateIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final Map<String, String> properties = ImmutableMap.of(
                "http_enable_tls", "true",
                "http_tls_key_file", privateKey.getAbsolutePath(),
                "http_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing HTTP X.509 certificate: ");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();
    }

    @Test
    public void testHttpPublishUriIsRelativeURI() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter http_publish_uri should be an absolute URI (found /foo)");

        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_publish_uri", "/foo"))).addConfigurationBean(configuration).process();
    }

    @Test
    public void testHttpExternalUriIsRelativeURI() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_external_uri", "/foo/"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpExternalUri()).isEqualTo(URI.create("/foo/"));
    }

    @Test
    public void testHttpPublishUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_publish_uri", "http://www.example.com:12900/foo/"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri()).isEqualTo(URI.create("http://www.example.com:12900/foo/"));
    }

    @Test
    public void testHttpPublishUriWithMissingTrailingSlash() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_publish_uri", "http://www.example.com:12900/foo"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri()).isEqualTo(URI.create("http://www.example.com:12900/foo/"));
    }

    @Test
    public void testHttpExternalUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_external_uri", "http://www.example.com:12900/foo/"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpExternalUri()).isEqualTo(URI.create("http://www.example.com:12900/foo/"));
    }

    @Test
    public void testHttpPublishUriWithHttpDefaultPort() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_publish_uri", "http://example.com/"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri()).hasPort(80);
    }

    @Test
    public void testHttpPublishUriWithCustomPort() throws RepositoryException, ValidationException {
        jadConfig.setRepository(new InMemoryRepository(ImmutableMap.of("http_publish_uri", "http://example.com:12900/"))).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri()).hasPort(12900);
    }

    @Test
    public void testHttpPublishUriWithCustomScheme() throws RepositoryException, ValidationException {
        final Map<String, String> properties = ImmutableMap.of(
                "http_publish_uri", "https://example.com:12900/",
                "http_enable_tls", "false");

        jadConfig.setRepository(new InMemoryRepository(properties)).addConfigurationBean(configuration).process();

        assertThat(configuration.getHttpPublishUri()).hasScheme("https");
    }
}
