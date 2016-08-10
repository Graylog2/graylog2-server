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
package org.graylog2;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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
    public void testRestListenUriIsRelativeURI() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "/foo");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter rest_listen_uri should be an absolute URI (found /foo)");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testWebListenUriIsRelativeURI() throws RepositoryException, ValidationException {
        validProperties.put("web_listen_uri", "/foo");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter web_listen_uri should be an absolute URI (found /foo)");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testRestListenUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://www.example.com:12900/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestListenUri()).isEqualTo(URI.create("http://www.example.com:12900/"));
    }

    @Test
    public void testWebListenUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        validProperties.put("web_listen_uri", "http://www.example.com:12900/web");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getWebListenUri()).isEqualTo(URI.create("http://www.example.com:12900/web/"));
    }

    @Test
    public void testRestListenUriWithHttpDefaultPort() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://example.com/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestListenUri()).hasPort(80);
    }

    @Test
    public void testRestListenUriWithCustomPort() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://example.com:12900/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestListenUri()).hasPort(12900);
    }

    @Test
    public void testWebListenUriWithHttpDefaultPort() throws RepositoryException, ValidationException {
        validProperties.put("web_listen_uri", "http://example.com/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getWebListenUri()).hasPort(80);
    }

    @Test
    public void testWebListenUriWithCustomPort() throws RepositoryException, ValidationException {
        validProperties.put("web_listen_uri", "http://example.com:9000/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getWebListenUri()).hasPort(9000);
    }

    @Test
    public void testPasswordSecretIsTooShort() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", "too short");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("The minimum length for \"password_secret\" is 16 characters.");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testPasswordSecretIsEmpty() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", "");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter password_secret should not be blank");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testPasswordSecretIsNull() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", null);

        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("Required parameter \"password_secret\" not found.");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testApiListenerOnRootAndWebListenerOnSubPath() throws ValidationException, RepositoryException {
        validProperties.put("rest_listen_uri", "http://0.0.0.0:12900/");
        validProperties.put("web_listen_uri", "http://0.0.0.0:12900/web/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestListenUri()).isEqualTo(URI.create("http://0.0.0.0:12900/"));
        assertThat(configuration.getWebListenUri()).isEqualTo(URI.create("http://0.0.0.0:12900/web/"));
    }

    @Test
    public void testWebListenerOnRootAndApiListenerOnSubPath() throws ValidationException, RepositoryException {
        validProperties.put("rest_listen_uri", "http://0.0.0.0:9000/api/");
        validProperties.put("web_listen_uri", "http://0.0.0.0:9000/");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getRestListenUri()).isEqualTo(URI.create("http://0.0.0.0:9000/api/"));
        assertThat(configuration.getWebListenUri()).isEqualTo(URI.create("http://0.0.0.0:9000/"));
    }

    @Test
    public void testPasswordSecretIsValid() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", "abcdefghijklmnopqrstuvwxyz");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getPasswordSecret()).isEqualTo("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void testRestApiListeningOnWildcardOnSamePortAsWebInterface() throws ValidationException, RepositoryException {
        validProperties.put("rest_listen_uri", "http://0.0.0.0:9000/api/");
        validProperties.put("web_listen_uri", "http://127.0.0.1:9000/");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Wildcard IP addresses cannot be used if the Graylog REST API and web interface listen on the same port.");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testWebInterfaceListeningOnWildcardOnSamePortAsRestApi() throws ValidationException, RepositoryException {
        validProperties.put("rest_listen_uri", "http://127.0.0.1:9000/api/");
        validProperties.put("web_listen_uri", "http://0.0.0.0:9000/");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Wildcard IP addresses cannot be used if the Graylog REST API and web interface listen on the same port.");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }
}
