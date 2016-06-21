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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.node.GraylogNode;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.esplugin.MonitorPlugin;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

@Singleton
public class EsNodeProvider implements Provider<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(EsNodeProvider.class);

    private final ElasticsearchConfiguration configuration;
    private final NodeId nodeId;

    @Inject
    public EsNodeProvider(ElasticsearchConfiguration configuration, NodeId nodeId) {
        this.configuration = requireNonNull(configuration);
        this.nodeId = requireNonNull(nodeId);
    }

    @Override
    public Node get() {
        return new GraylogNode(
                readNodeSettings(configuration, nodeId),
                Collections.<Class<? extends Plugin>>singleton(MonitorPlugin.class));
    }

    public static Settings readNodeSettings(ElasticsearchConfiguration conf, NodeId nodeId) {
        final Settings.Builder settings = Settings.builder();

        // Standard Configuration.
        settings.put("cluster.name", conf.getClusterName());

        settings.put("node.name", conf.getNodeNamePrefix() + nodeId);
        settings.put("node.master", conf.isMasterNode());
        settings.put("node.data", conf.isDataNode());
        settings.put("node.client", true);


        settings.put("path.home", conf.getPathHome());
        if (!isNullOrEmpty(conf.getPathData())) {
            settings.put("path.data", conf.getPathData());
        }

        settings.put("action.auto_create_index", false);

        settings.put("http.enabled", conf.isHttpEnabled());
        settings.put("transport.tcp.port", conf.getTransportTcpPort());

        settings.put("discovery.initial_state_timeout", conf.getInitialStateTimeout());

        final List<String> unicastHosts = conf.getUnicastHosts();
        if (unicastHosts != null && !unicastHosts.isEmpty()) {
            final String[] trimmedHosts = new String[unicastHosts.size()];
            for (int i = 0; i < unicastHosts.size(); i++) {
                final String host = unicastHosts.get(i);
                trimmedHosts[i] = host.trim();
            }
            settings.putArray("discovery.zen.ping.unicast.hosts", trimmedHosts);
        }

        if (!isNullOrEmpty(conf.getNetworkHost())) {
            settings.put("network.host", conf.getNetworkHost());
        }
        if (!isNullOrEmpty(conf.getNetworkBindHost())) {
            settings.put("network.bind_host", conf.getNetworkBindHost());
        }
        if (!isNullOrEmpty(conf.getNetworkPublishHost())) {
            settings.put("network.publish_host", conf.getNetworkPublishHost());
        }

        // Overwrite from a custom ElasticSearch config file.
        final Path esConfigFile = conf.getConfigFile();
        if (esConfigFile != null) {
            try {
                settings.loadFromPath(esConfigFile);
            } catch (SettingsException e) {
                LOG.warn("Cannot read Elasticsearch configuration from " + esConfigFile, e);
            }
        }

        return settings.build();
    }
}
