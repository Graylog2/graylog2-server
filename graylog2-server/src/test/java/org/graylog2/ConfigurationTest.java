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
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

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
    }

    @Test
    public void testWebListenUriIsAbsoluteURI() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://www.example.com:12900/web");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }
}