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

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
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

    @Before
    public void setUp() {
        configuration = new HttpConfiguration();
    }

    @Test
    public void testRestListenUriIsRelativeURI() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter rest_listen_uri should be an absolute URI (found /foo)");

        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_listen_uri", "/foo")), configuration).process();
    }

    @Test
    public void testWebListenUriIsRelativeURI() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter web_listen_uri should be an absolute URI (found /foo)");

        new JadConfig(new InMemoryRepository(ImmutableMap.of("web_listen_uri", "/foo")), configuration).process();
    }

    @Test
    public void testRestListenUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_listen_uri", "http://www.example.com:12900/")), configuration).process();

        assertThat(configuration.getRestListenUri()).isEqualTo(URI.create("http://www.example.com:12900/"));
    }

    @Test
    public void testWebListenUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("web_listen_uri", "http://www.example.com:12900/web")), configuration).process();

        assertThat(configuration.getWebListenUri()).isEqualTo(URI.create("http://www.example.com:12900/web/"));
    }

    @Test
    public void testRestListenUriWithHttpDefaultPort() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_listen_uri", "http://example.com/")), configuration).process();

        assertThat(configuration.getRestListenUri()).hasPort(80);
    }

    @Test
    public void testRestListenUriWithCustomPort() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_listen_uri", "http://example.com:12900/")), configuration).process();

        assertThat(configuration.getRestListenUri()).hasPort(12900);
    }

    @Test
    public void testWebListenUriWithHttpDefaultPort() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("web_listen_uri", "http://example.com/")), configuration).process();

        assertThat(configuration.getWebListenUri()).hasPort(80);
    }

    @Test
    public void testWebListenUriWithCustomPort() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("web_listen_uri", "http://example.com:9000/")), configuration).process();

        assertThat(configuration.getWebListenUri()).hasPort(9000);
    }

    @Test
    public void testApiListenerOnRootAndWebListenerOnSubPath() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://0.0.0.0:12900/",
                "web_listen_uri", "http://0.0.0.0:12900/web/");

        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertThat(configuration.getRestListenUri()).isEqualTo(URI.create("http://0.0.0.0:12900/"));
        assertThat(configuration.getWebListenUri()).isEqualTo(URI.create("http://0.0.0.0:12900/web/"));
    }

    @Test
    public void testWebListenerOnRootAndApiListenerOnSubPath() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://0.0.0.0:9000/api/",
                "web_listen_uri", "http://0.0.0.0:9000/");

        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertThat(configuration.getRestListenUri()).isEqualTo(URI.create("http://0.0.0.0:9000/api/"));
        assertThat(configuration.getWebListenUri()).isEqualTo(URI.create("http://0.0.0.0:9000/"));
    }


    @Test
    public void testRestApiListeningOnWildcardOnSamePortAsWebInterface() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://0.0.0.0:9000/api/",
                "web_listen_uri", "http://127.0.0.1:9000/");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Wildcard IP addresses cannot be used if the Graylog REST API and web interface listen on the same port.");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void testWebInterfaceListeningOnWildcardOnSamePortAsRestApi() throws ValidationException, RepositoryException {
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:9000/api/",
                "web_listen_uri", "http://0.0.0.0:9000/");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Wildcard IP addresses cannot be used if the Graylog REST API and web interface listen on the same port.");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void testRestTransportUriLocalhost() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_listen_uri", "http://127.0.0.1:12900")), configuration).process();

        assertThat(configuration.getDefaultRestTransportUri().toString()).isEqualTo("http://127.0.0.1:12900/");
    }

    @Test
    public void testRestListenUriWildcard() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_listen_uri", "http://0.0.0.0:12900")), configuration).process();

        Assert.assertNotEquals("http://0.0.0.0:12900", configuration.getDefaultRestTransportUri().toString());
        Assert.assertNotNull(configuration.getDefaultRestTransportUri());
    }

    @Test
    public void testRestTransportUriWildcard() throws RepositoryException, ValidationException {
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://0.0.0.0:12900",
                "rest_transport_uri", "http://0.0.0.0:12900");

        new JadConfig(new InMemoryRepository(properties), configuration).process();

        Assert.assertNotEquals(URI.create("http://0.0.0.0:12900"), configuration.getRestTransportUri());
    }

    @Test
    public void testRestTransportUriWildcardKeepsPath() throws RepositoryException, ValidationException {
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://0.0.0.0:12900/api/",
                "rest_transport_uri", "http://0.0.0.0:12900/api/");

        new JadConfig(new InMemoryRepository(properties), configuration).process();

        Assert.assertNotEquals(URI.create("http://0.0.0.0:12900/api/"), configuration.getRestTransportUri());
        assertThat(configuration.getRestTransportUri().getPath()).isEqualTo("/api/");
    }

    @Test
    public void testRestTransportUriCustom() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_listen_uri", "http://10.0.0.1:12900")), configuration).process();

        assertThat(configuration.getDefaultRestTransportUri().toString()).isEqualTo("http://10.0.0.1:12900/");
    }

    @Test
    public void testGetRestUriScheme() throws RepositoryException, ValidationException, IOException {
        final HttpConfiguration configWithoutTls = new HttpConfiguration();
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_enable_tls", "false")), configWithoutTls).process();

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "rest_enable_tls", "true",
                "rest_tls_key_file", temporaryFolder.newFile("graylog.key").getAbsolutePath(),
                "rest_tls_cert_file", temporaryFolder.newFile("graylog.crt").getAbsolutePath());
        final HttpConfiguration configWithTls = new HttpConfiguration();
        new JadConfig(new InMemoryRepository(properties), configWithTls).process();

        assertThat(configWithoutTls.getRestUriScheme()).isEqualTo("http");
        assertThat(configWithTls.getRestUriScheme()).isEqualTo("https");
    }

    @Test
    public void testGetWebUriScheme() throws RepositoryException, ValidationException, IOException {
        final HttpConfiguration configWithoutTls = new HttpConfiguration();
        new JadConfig(new InMemoryRepository(ImmutableMap.of("web_enable_tls", "false")), configWithoutTls).process();

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "web_enable_tls", "true",
                "web_tls_key_file", temporaryFolder.newFile("graylog.key").getAbsolutePath(),
                "web_tls_cert_file", temporaryFolder.newFile("graylog.crt").getAbsolutePath());
        final HttpConfiguration configWithTls = new HttpConfiguration();
        new JadConfig(new InMemoryRepository(properties), configWithTls).process();

        assertThat(configWithoutTls.getWebUriScheme()).isEqualTo("http");
        assertThat(configWithTls.getWebUriScheme()).isEqualTo("https");
    }

    @Test
    public void restTlsValidationFailsIfPrivateKeyIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_enable_tls", "true",
                "rest_tls_key_file", privateKey.getAbsolutePath(),
                "rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API private key: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void restTlsValidationFailsIfPrivateKeyIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFolder("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_enable_tls", "true",
                "rest_tls_key_file", privateKey.getAbsolutePath(),
                "rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API private key: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void restTlsValidationFailsIfPrivateKeyIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_enable_tls", "true",
                "rest_tls_key_file", privateKey.getAbsolutePath(),
                "rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API private key: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void restTlsValidationFailsIfCertificateIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_enable_tls", "true",
                "rest_tls_key_file", privateKey.getAbsolutePath(),
                "rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API X.509 certificate: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void restTlsValidationFailsIfCertificateIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFolder("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_enable_tls", "true",
                "rest_tls_key_file", privateKey.getAbsolutePath(),
                "rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API X.509 certificate: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void restTlsValidationFailsIfCertificateIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_enable_tls", "true",
                "rest_tls_key_file", privateKey.getAbsolutePath(),
                "rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API X.509 certificate: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void webTlsValidationFailsIfPrivateKeyIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "web_enable_tls", "true",
                "web_tls_key_file", privateKey.getAbsolutePath(),
                "web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface private key: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void webTlsValidationFailsIfPrivateKeyIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFolder("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "web_enable_tls", "true",
                "web_tls_key_file", privateKey.getAbsolutePath(),
                "web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface private key: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void webTlsValidationFailsIfPrivateKeyIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "web_enable_tls", "true",
                "web_tls_key_file", privateKey.getAbsolutePath(),
                "web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface private key: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void webTlsValidationFailsIfCertificateIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "web_enable_tls", "true",
                "web_tls_key_file", privateKey.getAbsolutePath(),
                "web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface X.509 certificate: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void webTlsValidationFailsIfCertificateIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFolder("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "web_enable_tls", "true",
                "web_tls_key_file", privateKey.getAbsolutePath(),
                "web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface X.509 certificate: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void webTlsValidationFailsIfCertificateIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_listen_uri", "http://127.0.0.1:12900/",
                "web_enable_tls", "true",
                "web_tls_key_file", privateKey.getAbsolutePath(),
                "web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface X.509 certificate: ");

        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void testRestTransportUriIsRelativeURI() throws RepositoryException, ValidationException {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter rest_transport_uri should be an absolute URI (found /foo)");


        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_transport_uri", "/foo")), configuration).process();
    }

    @Test
    public void testWebEndpointUriIsRelativeURI() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("web_endpoint_uri", "/foo")), configuration).process();

        assertThat(configuration.getWebEndpointUri()).isEqualTo(URI.create("/foo"));
    }

    @Test
    public void testRestTransportUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_transport_uri", "http://www.example.com:12900/foo")), configuration).process();

        assertThat(configuration.getRestTransportUri()).isEqualTo(URI.create("http://www.example.com:12900/foo/"));
    }

    @Test
    public void testWebEndpointUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("web_endpoint_uri", "http://www.example.com:12900/foo")), configuration).process();

        assertThat(configuration.getWebEndpointUri()).isEqualTo(URI.create("http://www.example.com:12900/foo"));
    }

    @Test
    public void testRestTransportUriWithHttpDefaultPort() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_transport_uri", "http://example.com/")), configuration).process();

        assertThat(configuration.getRestTransportUri()).hasPort(80);
    }

    @Test
    public void testRestTransportUriWithCustomPort() throws RepositoryException, ValidationException {
        new JadConfig(new InMemoryRepository(ImmutableMap.of("rest_transport_uri", "http://example.com:12900/")), configuration).process();

        assertThat(configuration.getRestTransportUri()).hasPort(12900);
    }

    @Test
    public void testRestTransportUriWithCustomScheme() throws RepositoryException, ValidationException {
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "rest_transport_uri", "https://example.com:12900/",
                "rest_enable_tls", "false");

        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertThat(configuration.getRestTransportUri()).hasScheme("https");
    }

    @Test
    public void testRestListenUriAndWebListenUriWithSameScheme() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        final Map<String, String> properties = ImmutableMap.<String, String>builder()
                .put("rest_listen_uri", "https://127.0.0.1:8000/api")
                .put("rest_transport_uri", "https://127.0.0.1:8000/api")
                .put("rest_enable_tls", "true")
                .put("rest_tls_key_file", privateKey.getAbsolutePath())
                .put("rest_tls_cert_file", certificate.getAbsolutePath())
                .put("web_listen_uri", "https://127.0.0.1:8000/")
                .put("web_enable_tls", "true")
                .build();


        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertThat(configuration.getRestListenUri()).hasScheme("https");
        assertThat(configuration.getRestTransportUri()).hasScheme("https");
        assertThat(configuration.getWebListenUri()).hasScheme("https");
    }
}