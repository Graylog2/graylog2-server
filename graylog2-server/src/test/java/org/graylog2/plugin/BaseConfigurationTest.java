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
package org.graylog2.plugin;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class BaseConfigurationTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private class Configuration extends BaseConfiguration {
        @Parameter(value = "rest_listen_uri", required = true)
        private URI restListenUri = URI.create("http://127.0.0.1:12900/");

        @Parameter(value = "web_listen_uri", required = true)
        private URI webListenUri = URI.create("http://127.0.0.1:9000/");

        @Parameter(value = "node_id_file", required = false)
        private String nodeIdFile = "/etc/graylog/server/node-id";

        @Override
        public String getNodeIdFile() {
            return nodeIdFile;
        }

        @Override
        public URI getRestListenUri() {
            return Tools.getUriWithPort(restListenUri, BaseConfiguration.GRAYLOG_DEFAULT_PORT);
        }

        @Override
        public URI getWebListenUri() {
            return Tools.getUriWithPort(webListenUri, BaseConfiguration.GRAYLOG_DEFAULT_WEB_PORT);
        }
    }

    private Map<String, String> validProperties;

    @Before
    public void setUp() throws Exception {
        validProperties = new HashMap<>();

        // Required properties
        validProperties.put("password_secret", "ipNUnWxmBLCxTEzXcyamrdy0Q3G7HxdKsAvyg30R9SCof0JydiZFiA3dLSkRsbLF");
        // SHA-256 of "admin"
        validProperties.put("root_password_sha2", "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918");
    }

    @Test
    public void testRestTransportUriLocalhost() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://127.0.0.1:12900");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("http://127.0.0.1:12900", configuration.getDefaultRestTransportUri().toString());
    }

    @Test
    public void testRestListenUriWildcard() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://0.0.0.0:12900");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNotEquals("http://0.0.0.0:12900", configuration.getDefaultRestTransportUri().toString());
        Assert.assertNotNull(configuration.getDefaultRestTransportUri());
    }

    @Test
    public void testRestTransportUriWildcard() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://0.0.0.0:12900");
        validProperties.put("rest_transport_uri", "http://0.0.0.0:12900");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNotEquals(URI.create("http://0.0.0.0:12900"), configuration.getRestTransportUri());
    }

    @Test
    public void testRestTransportUriWildcardKeepsPath() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://0.0.0.0:12900/api/");
        validProperties.put("rest_transport_uri", "http://0.0.0.0:12900/api/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNotEquals(URI.create("http://0.0.0.0:12900/api/"), configuration.getRestTransportUri());
        Assert.assertEquals("/api/", configuration.getRestTransportUri().getPath());
    }

    @Test
    public void testRestTransportUriCustom() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://10.0.0.1:12900");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("http://10.0.0.1:12900", configuration.getDefaultRestTransportUri().toString());
    }

    @Test
    public void testGetRestUriScheme() throws RepositoryException, ValidationException, IOException {
        validProperties.put("rest_enable_tls", "false");
        final Configuration configWithoutTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithoutTls).process();

        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", temporaryFolder.newFile("graylog.key").getAbsolutePath());
        validProperties.put("rest_tls_cert_file", temporaryFolder.newFile("graylog.crt").getAbsolutePath());
        final Configuration configWithTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithTls).process();

        assertEquals("http", configWithoutTls.getRestUriScheme());
        assertEquals("https", configWithTls.getRestUriScheme());
    }

    @Test
    public void testGetWebUriScheme() throws RepositoryException, ValidationException, IOException {
        validProperties.put("web_enable_tls", "false");
        final Configuration configWithoutTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithoutTls).process();

        validProperties.put("web_enable_tls", "true");
        validProperties.put("web_tls_key_file", temporaryFolder.newFile("graylog.key").getAbsolutePath());
        validProperties.put("web_tls_cert_file", temporaryFolder.newFile("graylog.crt").getAbsolutePath());
        final Configuration configWithTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithTls).process();

        assertEquals("http", configWithoutTls.getWebUriScheme());
        assertEquals("https", configWithTls.getWebUriScheme());
    }

    @Test
    public void restTlsValidationFailsIfPrivateKeyIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API private key: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void restTlsValidationFailsIfPrivateKeyIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFolder("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API private key: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void restTlsValidationFailsIfPrivateKeyIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API private key: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void restTlsValidationFailsIfCertificateIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API X.509 certificate: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void restTlsValidationFailsIfCertificateIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFolder("graylog.crt");

        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API X.509 certificate: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void restTlsValidationFailsIfCertificateIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("rest_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing REST API X.509 certificate: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void webTlsValidationFailsIfPrivateKeyIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("web_enable_tls", "true");
        validProperties.put("web_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface private key: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void webTlsValidationFailsIfPrivateKeyIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFolder("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("web_enable_tls", "true");
        validProperties.put("web_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface private key: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void webTlsValidationFailsIfPrivateKeyIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("web_enable_tls", "true");
        validProperties.put("web_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(privateKey.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface private key: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void webTlsValidationFailsIfCertificateIsMissing() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("web_enable_tls", "true");
        validProperties.put("web_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.delete()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface X.509 certificate: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void webTlsValidationFailsIfCertificateIsDirectory() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFolder("graylog.crt");

        validProperties.put("web_enable_tls", "true");
        validProperties.put("web_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.isDirectory()).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface X.509 certificate: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void webTlsValidationFailsIfCertificateIsUnreadable() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("web_enable_tls", "true");
        validProperties.put("web_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("web_tls_cert_file", certificate.getAbsolutePath());

        assertThat(certificate.setReadable(false, false)).isTrue();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Unreadable or missing web interface X.509 certificate: ");

        new JadConfig(new InMemoryRepository(validProperties), new Configuration()).process();
    }

    @Test
    public void testRestTransportUriIsRelativeURI() throws RepositoryException, ValidationException {
        validProperties.put("rest_transport_uri", "/foo");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter rest_transport_uri should be an absolute URI (found /foo)");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testWebEndpointUriIsRelativeURI() throws RepositoryException, ValidationException {
        validProperties.put("web_endpoint_uri", "/foo");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertEquals(URI.create("/foo"), configuration.getWebEndpointUri());
    }

    @Test
    public void testRestTransportUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        validProperties.put("rest_transport_uri", "http://www.example.com:12900/foo");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertEquals(URI.create("http://www.example.com:12900/foo/"), configuration.getRestTransportUri());
    }

    @Test
    public void testWebEndpointUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        validProperties.put("web_endpoint_uri", "http://www.example.com:12900/foo");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertEquals(URI.create("http://www.example.com:12900/foo"), configuration.getWebEndpointUri());
    }

    @Test
    public void testRestTransportUriWithHttpDefaultPort() throws RepositoryException, ValidationException {
        validProperties.put("rest_transport_uri", "http://example.com/");

        org.graylog2.Configuration configuration = new org.graylog2.Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestTransportUri()).hasPort(80);
    }

    @Test
    public void testRestTransportUriWithCustomPort() throws RepositoryException, ValidationException {
        validProperties.put("rest_transport_uri", "http://example.com:12900/");

        org.graylog2.Configuration configuration = new org.graylog2.Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestTransportUri()).hasPort(12900);
    }

    @Test
    public void testRestTransportUriWithCustomScheme() throws RepositoryException, ValidationException {
        validProperties.put("rest_transport_uri", "https://example.com:12900/");
        validProperties.put("rest_enable_tls", "false");

        org.graylog2.Configuration configuration = new org.graylog2.Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestTransportUri()).hasScheme("https");
    }

    @Test
    public void testRestListenUriAndWebListenUriWithSameScheme() throws Exception {
        final File privateKey = temporaryFolder.newFile("graylog.key");
        final File certificate = temporaryFolder.newFile("graylog.crt");

        validProperties.put("rest_listen_uri", "https://127.0.0.1:8000/api");
        validProperties.put("rest_transport_uri", "https://127.0.0.1:8000/api");
        validProperties.put("rest_enable_tls", "true");
        validProperties.put("rest_tls_key_file", privateKey.getAbsolutePath());
        validProperties.put("rest_tls_cert_file", certificate.getAbsolutePath());
        validProperties.put("web_listen_uri", "https://127.0.0.1:8000/");
        validProperties.put("web_enable_tls", "true");

        org.graylog2.Configuration configuration = new org.graylog2.Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestListenUri()).hasScheme("https");
        assertThat(configuration.getRestTransportUri()).hasScheme("https");
        assertThat(configuration.getWebListenUri()).hasScheme("https");
    }
}
