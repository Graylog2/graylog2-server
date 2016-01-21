/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BaseConfigurationTest {
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
    private File tempFile;

    @Before
    public void setUp() throws Exception {
        validProperties = Maps.newHashMap();
        tempFile = File.createTempFile("graylog", null);

        // Required properties
        validProperties.put("password_secret", "ipNUnWxmBLCxTEzXcyamrdy0Q3G7HxdKsAvyg30R9SCof0JydiZFiA3dLSkRsbLF");
        validProperties.put("elasticsearch_config_file", tempFile.getAbsolutePath());
        validProperties.put("use_gelf", "true");
        validProperties.put("gelf_listen_port", "12201");
        validProperties.put("root_password_sha2", "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"); // sha2 of admin
    }

    @After
    public void tearDown() {
        if(tempFile != null) {
            tempFile.delete();
        }
    }

    @Test
    public void testRestTransportUriLocalhost() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://127.0.0.1:12900");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("http://127.0.0.1:12900", configuration.getDefaultRestTransportUri().toString());
    }

    @Test
    public void testRestTransportUriWildcard() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://0.0.0.0:12900");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNotEquals("http://0.0.0.0:12900", configuration.getDefaultRestTransportUri().toString());
        Assert.assertNotNull(configuration.getDefaultRestTransportUri());
    }

    @Test
    public void testRestTransportUriCustom() throws RepositoryException, ValidationException {
        validProperties.put("rest_listen_uri", "http://10.0.0.1:12900");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("http://10.0.0.1:12900", configuration.getDefaultRestTransportUri().toString());
    }

    @Test
    public void testGetRestUriScheme() throws RepositoryException, ValidationException {
        validProperties.put("rest_enable_tls", "false");
        final Configuration configWithoutTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithoutTls).process();

        validProperties.put("rest_enable_tls", "true");
        final Configuration configWithTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithTls).process();

        assertEquals("http", configWithoutTls.getRestUriScheme());
        assertEquals("https", configWithTls.getRestUriScheme());
    }

    @Test
    public void testGetWebUriScheme() throws RepositoryException, ValidationException {
        validProperties.put("web_enable_tls", "false");
        final Configuration configWithoutTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithoutTls).process();

        validProperties.put("web_enable_tls", "true");
        final Configuration configWithTls = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configWithTls).process();

        assertEquals("http", configWithoutTls.getWebUriScheme());
        assertEquals("https", configWithTls.getWebUriScheme());
    }
}
