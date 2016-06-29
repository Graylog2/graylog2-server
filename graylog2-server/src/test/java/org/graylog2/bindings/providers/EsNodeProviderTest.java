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
package org.graylog2.bindings.providers;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.elasticsearch.common.settings.Settings;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.plugin.system.NodeId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.AssertNotEquals.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EsNodeProviderTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeId nodeId;

    @Before
    public void setUp() throws IOException {
        nodeId = new NodeId(temporaryFolder.newFile().getAbsolutePath());
    }

    private ElasticsearchConfiguration setupConfig(Map<String, String> settings) {
        // required params we don't care about in this test, so we set them to dummy values for all test cases
        settings.put("retention_strategy", "delete");

        ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
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
        ElasticsearchConfiguration defaultConfig = setupConfig(minimalSettings);

        Map<String, String> settings = Maps.newHashMap();
        ElasticsearchConfiguration config = setupConfig(settings);

        Settings nodeSettings = EsNodeProvider.readNodeSettings(config, nodeId);

        assertEquals(defaultConfig.getClusterName(), nodeSettings.get("cluster.name"));
        assertEquals(defaultConfig.getNodeNamePrefix() + nodeId, nodeSettings.get("node.name"));
        assertEquals(defaultConfig.isMasterNode(), nodeSettings.getAsBoolean("node.master", false));
        assertEquals(defaultConfig.isDataNode(), nodeSettings.getAsBoolean("node.data", false));
        assertEquals(defaultConfig.isHttpEnabled(), nodeSettings.getAsBoolean("http.enabled", false));
        assertEquals(defaultConfig.getTransportTcpPort(), nodeSettings.getAsInt("transport.tcp.port", 0).intValue());
        assertEquals(defaultConfig.getInitialStateTimeout(), nodeSettings.get("discovery.initial_state_timeout"));
        assertEquals(false, nodeSettings.getAsBoolean("action.auto_create_index", true));

    }

    @Test
    public void noEsFileValuesFromGraylog2Config() {
        // checks that all values in the node settings are from the graylog2 conf and that no extra settings remain untested
        Map<String, String> settings = Maps.newHashMap();
        Map<String, String> esPropNames = Maps.newHashMap();

        // add all ES settings here that are configurable via graylog.conf
        addEsConfig(esPropNames, settings, "cluster.name", "elasticsearch_cluster_name", "garylog5");
        addEsConfig(esPropNames, settings, "node.name", "elasticsearch_node_name_prefix", "garylord-");
        addEsConfig(esPropNames, settings, "node.master", "elasticsearch_node_master", "true");
        addEsConfig(esPropNames, settings, "node.data", "elasticsearch_node_data", "true");
        addEsConfig(esPropNames, settings, "path.home", "elasticsearch_path_home", "data/elasticsearch");
        addEsConfig(esPropNames, settings, "path.data", "elasticsearch_path_data", "data/elasticsearch");
        addEsConfig(esPropNames, settings, "transport.tcp.port", "elasticsearch_transport_tcp_port", "9999");
        addEsConfig(esPropNames, settings, "http.enabled", "elasticsearch_http_enabled", "true");
        addEsConfig(esPropNames,
                settings,
                "discovery.zen.ping.unicast.hosts.0",
                "elasticsearch_discovery_zen_ping_unicast_hosts",
                "example.net");
        addEsConfig(esPropNames,
                settings,
                "discovery.initial_state_timeout",
                "elasticsearch_discovery_initial_state_timeout",
                "5s");
        esPropNames.put("action.auto_create_index", "false");
        esPropNames.put("node.client", "true");

        ElasticsearchConfiguration config = setupConfig(settings);

        Settings nodeSettings = EsNodeProvider.readNodeSettings(config, nodeId);

        assertThat(Sets.symmetricDifference(esPropNames.keySet(), nodeSettings.getAsMap().keySet())).isEmpty();

        assertThat(nodeSettings.get("node.name")).isEqualTo(settings.get("elasticsearch_node_name_prefix") + nodeId);
        for (Map.Entry<String, String> property : esPropNames.entrySet()) {
            final String key = property.getKey();
            // The node name is being constructed and not used verbatim.
            if ("node.name".equals(key)) {
                continue;
            }
            final String settingValue = nodeSettings.get(key);

            // the node setting value should be whatever we have put in.
            assertThat(settingValue).isEqualTo(property.getValue());
        }
    }

    @Test
    public void testEsConfFileOverride() throws IOException, URISyntaxException {
        final Map<String, String> settings = Maps.newHashMap();

        final String esConfigFilePath = new File(Resources.getResource("org/graylog2/bindings/providers/elasticsearch.yml").toURI()).getAbsolutePath();
        settings.put("elasticsearch_config_file", esConfigFilePath);

        ElasticsearchConfiguration config = setupConfig(settings);

        final Settings nodeSettings = EsNodeProvider.readNodeSettings(config, nodeId);

        assertNotEquals("cluster.name", config.getClusterName(), nodeSettings.get("cluster.name"));
        assertEquals("cluster.name", "fromfile", nodeSettings.get("cluster.name"));
        assertNotEquals("node.name", config.getNodeNamePrefix(), nodeSettings.get("node.name"));
        assertEquals("node.name", "filenode", nodeSettings.get("node.name"));
        assertNotEquals("node.master", config.isMasterNode(), nodeSettings.get("node.master"));
        assertNotEquals("node.data", config.isDataNode(), nodeSettings.get("node.data"));
        assertNotEquals("http.enabled", config.isHttpEnabled(), nodeSettings.get("http.enabled"));
        assertNotEquals("transport.tcp.port", config.getTransportTcpPort(), nodeSettings.get("transport.tcp.port"));
        assertNotEquals("discovery.initial_state_timeout", config.getInitialStateTimeout(), nodeSettings.get("discovery.initial_state_timeout"));
        assertNotEquals("discovery.zen.ping.unicast.hosts", config.getUnicastHosts(),
                Lists.newArrayList(nodeSettings.getAsArray("discovery.zen.ping.unicast.hosts")));
    }

    @Test
    public void singletonListZenUnicastHostsWorks() throws IOException, ValidationException, RepositoryException {
        Map<String, String> settings = ImmutableMap.of(
                "password_secret", "thisisatest",
                "retention_strategy", "delete",
                "root_password_sha2", "thisisatest",
                "elasticsearch_discovery_zen_ping_unicast_hosts", "example.com");

        final ElasticsearchConfiguration config = new ElasticsearchConfiguration();
        new JadConfig(new InMemoryRepository(settings), config).process();

        final Settings nodeSettings = EsNodeProvider.readNodeSettings(config, nodeId);

        assertThat(nodeSettings.getAsArray("discovery.zen.ping.unicast.hosts")).contains("example.com");
    }

    @Test
    public void zenUnicastHostsAreTrimmed() throws IOException, ValidationException, RepositoryException {
        Map<String, String> settings = ImmutableMap.of(
                "password_secret", "thisisatest",
                "retention_strategy", "delete",
                "root_password_sha2", "thisisatest",
                "elasticsearch_discovery_zen_ping_unicast_hosts", " example.com,   example.net ");

        final ElasticsearchConfiguration config = new ElasticsearchConfiguration();
        new JadConfig(new InMemoryRepository(settings), config).process();

        final Settings nodeSettings = EsNodeProvider.readNodeSettings(config, nodeId);

        assertThat(nodeSettings.getAsArray("discovery.zen.ping.unicast.hosts")).contains("example.com", "example.net");
    }

    private void addEsConfig(Map<String, String> esProps, Map<String, String> settings, String esName, String confName, String value) {
        esProps.put(esName, value);
        settings.put(confName, value);
    }
}
