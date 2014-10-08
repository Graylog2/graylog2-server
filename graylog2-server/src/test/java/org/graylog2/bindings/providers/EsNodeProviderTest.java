/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bindings.providers;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.graylog2.Configuration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.graylog2.AssertNotEquals.assertNotEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class EsNodeProviderTest {
    private Configuration setupConfig(Map<String, String> settings) {
        // required params we don't care about in this test, so we set them to dummy values for all test cases
        settings.put("password_secret", "thisisatest");
        settings.put("retention_strategy", "delete");
        settings.put("root_password_sha2", "thisisatest");

        Configuration configuration = new Configuration();
        try {
            new JadConfig(new InMemoryRepository(settings), configuration).process();
        } catch (ValidationException | RepositoryException e) {
            fail(e.getMessage());
        }
        return configuration;
    }

    @Test
    public void defaultConfigNoEsFile() {
        // check that all ES settings will be taken from the default values in Configuration.java if nothing is specified.
        Map<String, String> minimalSettings = Maps.newHashMap();
        Configuration defaultConfig = setupConfig(minimalSettings);

        Map<String, String> settings = Maps.newHashMap();
        Configuration config = setupConfig(settings);

        Map<String, String> nodeSettings = EsNodeProvider.readNodeSettings(config);

        assertEquals(defaultConfig.getEsClusterName(), nodeSettings.get("cluster.name"));
        assertEquals(defaultConfig.getEsNodeName(), nodeSettings.get("node.name"));
        assertEquals(Boolean.toString(defaultConfig.isEsIsMasterEligible()), nodeSettings.get("node.master"));
        assertEquals(Boolean.toString(defaultConfig.isEsStoreData()), nodeSettings.get("node.data"));
        assertEquals(Boolean.toString(defaultConfig.isEsIsHttpEnabled()), nodeSettings.get("http.enabled"));
        assertEquals(String.valueOf(defaultConfig.getEsTransportTcpPort()), nodeSettings.get("transport.tcp.port"));
        assertEquals(defaultConfig.getEsInitialStateTimeout(), nodeSettings.get("discovery.initial_state_timeout"));
        assertEquals(Boolean.toString(defaultConfig.isEsMulticastDiscovery()),
                nodeSettings.get("discovery.zen.ping.multicast.enabled"));
        assertEquals(Boolean.toString(false), nodeSettings.get("action.auto_create_index"));

    }

    @Test
    public void noEsFileValuesFromGraylog2Config() {
        // checks that all values in the node settings are from the graylog2 conf and that no extra settings remain untested
        Map<String, String> settings = Maps.newHashMap();
        Map<String, String> esPropNames = Maps.newHashMap();

        // add all ES settings here that are configurable via graylog2.conf
        addEsConfig(esPropNames, settings, "cluster.name", "elasticsearch_cluster_name", "garylog5");
        addEsConfig(esPropNames, settings, "node.name", "elasticsearch_node_name", "garylord");
        addEsConfig(esPropNames, settings, "node.master", "elasticsearch_node_master", "true");
        addEsConfig(esPropNames, settings, "node.data", "elasticsearch_node_data", "true");
        addEsConfig(esPropNames, settings, "transport.tcp.port", "elasticsearch_transport_tcp_port", "9999");
        addEsConfig(esPropNames, settings, "http.enabled", "elasticsearch_http_enabled", "true");
        addEsConfig(esPropNames,
                settings,
                "discovery.zen.ping.multicast.enabled",
                "elasticsearch_discovery_zen_ping_multicast_enabled",
                "false");
        addEsConfig(esPropNames,
                settings,
                "discovery.zen.ping.unicast.hosts",
                "elasticsearch_discovery_zen_ping_unicast_hosts",
                "192.168.1.1,192.168.2.1");
        addEsConfig(esPropNames,
                settings,
                "discovery.initial_state_timeout",
                "elasticsearch_discovery_initial_state_timeout",
                "5s");
        esPropNames.put("action.auto_create_index", "false");

        Configuration config = setupConfig(settings);

        Map<String, String> nodeSettings = EsNodeProvider.readNodeSettings(config);

        for (Map.Entry<String, String> property : esPropNames.entrySet()) {
            // remove the setting, so we can check if someone added new ones without testing them...
            String settingValue = nodeSettings.remove(property.getKey());
            // the node setting value should be whatever we have put in.
            assertEquals(property.getKey() + " values", property.getValue(), settingValue);
        }
        assertTrue("Untested properties remain: " + nodeSettings.keySet().toString(), nodeSettings.isEmpty());
    }

    @Test
    public void testEsConfFileOverride() throws IOException, URISyntaxException {
        final Map<String, String> settings = Maps.newHashMap();

        final String esConfigFilePath = new File(Resources.getResource("org/graylog2/bindings/providers/elasticsearch.yml").toURI()).getAbsolutePath();
        settings.put("elasticsearch_config_file", esConfigFilePath);

        Configuration config = setupConfig(settings);

        final Map<String, String> nodeSettings = EsNodeProvider.readNodeSettings(config);

        assertNotEquals("cluster.name", config.getEsClusterName(), nodeSettings.get("cluster.name"));
        assertNotEquals("node.name", config.getEsNodeName(), nodeSettings.get("node.name"));
        assertNotEquals("node.master", Boolean.toString(config.isEsIsMasterEligible()), nodeSettings.get("node.master"));
        assertNotEquals("node.data", Boolean.toString(config.isEsStoreData()), nodeSettings.get("node.data"));
        assertNotEquals("http.enabled", Boolean.toString(config.isEsIsHttpEnabled()), nodeSettings.get("http.enabled"));
        assertNotEquals("transport.tcp.port", String.valueOf(config.getEsTransportTcpPort()), nodeSettings.get("transport.tcp.port"));
        assertNotEquals("discovery.initial_state_timeout", config.getEsInitialStateTimeout(), nodeSettings.get("discovery.initial_state_timeout"));
        assertNotEquals("discovery.zen.ping.multicast.enabled", Boolean.toString(config.isEsMulticastDiscovery()),
                nodeSettings.get("discovery.zen.ping.multicast.enabled"));
        assertNotEquals("discovery.zen.ping.unicast.hosts",
                config.getEsUnicastHosts(),
                Lists.newArrayList(Splitter.on(",").split(nodeSettings.get("discovery.zen.ping.unicast.hosts"))));
    }

    @Test
    public void singletonListZenUnicastHostsWorks() throws IOException, ValidationException, RepositoryException {
        Map<String, String> settings = ImmutableMap.of(
                "password_secret", "thisisatest",
                "retention_strategy", "delete",
                "root_password_sha2", "thisisatest",
                "elasticsearch_discovery_zen_ping_unicast_hosts", "example.com");

        final Configuration config = new Configuration();
        new JadConfig(new InMemoryRepository(settings), config).process();

        final Map<String, String> nodeSettings = EsNodeProvider.readNodeSettings(config);

        assertEquals("discovery.zen.ping.unicast.hosts", nodeSettings.get("discovery.zen.ping.unicast.hosts"), "example.com");
    }

    @Test
    public void zenUnicastHostsAreTrimmed() throws IOException, ValidationException, RepositoryException {
        Map<String, String> settings = ImmutableMap.of(
                "password_secret", "thisisatest",
                "retention_strategy", "delete",
                "root_password_sha2", "thisisatest",
                "elasticsearch_discovery_zen_ping_unicast_hosts", " example.com,   example.net ");

        final Configuration config = new Configuration();
        new JadConfig(new InMemoryRepository(settings), config).process();

        final Map<String, String> nodeSettings = EsNodeProvider.readNodeSettings(config);

        assertEquals("discovery.zen.ping.unicast.hosts", nodeSettings.get("discovery.zen.ping.unicast.hosts"), "example.com,example.net");
    }

    private void addEsConfig(Map<String, String> esProps, Map<String, String> settings, String esName, String confName, String value) {
        esProps.put(esName, value);
        settings.put(confName, value);
    }
}