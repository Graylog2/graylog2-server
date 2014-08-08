package org.graylog2.shared;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class BaseConfigurationTest {
    private class Configuration extends BaseConfiguration {
        @Parameter(value = "rest_listen_uri", required = true)
        private String restListenUri = "http://127.0.0.1:12900/";

        @Parameter(value = "node_id_file", required = false)
        private String nodeIdFile = "/etc/graylog2-server-node-id";

        @Override
        public String getNodeIdFile() {
            return nodeIdFile;
        }

        @Override
        public URI getRestListenUri() {
            return Tools.getUriStandard(restListenUri);
        }
    }

    private Map<String, String> validProperties;
    private File tempFile;

    @BeforeMethod
    public void setUp() throws Exception {
        validProperties = Maps.newHashMap();

        try {
            tempFile = File.createTempFile("graylog", null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Required properties
        validProperties.put("password_secret", "ipNUnWxmBLCxTEzXcyamrdy0Q3G7HxdKsAvyg30R9SCof0JydiZFiA3dLSkRsbLF");
        validProperties.put("elasticsearch_config_file", tempFile.getAbsolutePath());
        validProperties.put("mongodb_useauth", "true");
        validProperties.put("mongodb_user", "user");
        validProperties.put("mongodb_password", "pass");
        validProperties.put("mongodb_database", "test");
        validProperties.put("mongodb_host", "localhost");
        validProperties.put("mongodb_port", "27017");
        validProperties.put("use_gelf", "true");
        validProperties.put("gelf_listen_port", "12201");
        validProperties.put("root_password_sha2", "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"); // sha2 of admin
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
}