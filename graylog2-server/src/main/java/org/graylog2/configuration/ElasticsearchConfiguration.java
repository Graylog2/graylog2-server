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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.FileReadableValidator;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import org.joda.time.Period;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ElasticsearchConfiguration {
    @Parameter(value = "elasticsearch_cluster_name")
    private String clusterName = "graylog2";

    @Parameter(value = "elasticsearch_node_name")
    private String nodeName = "graylog2-server";

    @Parameter(value = "elasticsearch_node_master")
    private boolean masterNode = false;

    @Parameter(value = "elasticsearch_node_data")
    private boolean dataNode = false;

    @Parameter(value = "elasticsearch_path_data")
    private String pathData = "data/elasticsearch";

    @Parameter(value = "elasticsearch_transport_tcp_port", validator = InetPortValidator.class)
    private int transportTcpPort = 9350;

    @Parameter(value = "elasticsearch_http_enabled")
    private boolean httpEnabled = false;

    @Parameter(value = "elasticsearch_discovery_zen_ping_multicast_enabled")
    private boolean multicastDiscovery = true;

    @Parameter(value = "elasticsearch_discovery_zen_ping_unicast_hosts", converter = StringListConverter.class)
    private List<String> unicastHosts;

    @Parameter(value = "elasticsearch_discovery_initial_state_timeout")
    private String initialStateTimeout = "3s";

    @Parameter(value = "elasticsearch_network_host")
    private String networkHost;

    @Parameter(value = "elasticsearch_network_bind_host")
    private String networkBindHost;

    @Parameter(value = "elasticsearch_network_publish_host")
    private String networkPublishHost;

    @Parameter(value = "elasticsearch_cluster_discovery_timeout", validator = PositiveLongValidator.class)
    private long clusterDiscoveryTimeout = 5000;

    @Parameter(value = "elasticsearch_disable_version_check")
    private boolean disableVersionCheck = false;

    @Parameter(value = "elasticsearch_config_file", validator = FileReadableValidator.class)
    private File configFile; // = "/etc/graylog/server/elasticsearch.yml";

    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String indexPrefix = "graylog2";

    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validator = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    @Parameter(value = "elasticsearch_max_docs_per_index", validator = PositiveIntegerValidator.class, required = true)
    private int maxDocsPerIndex = 80000000;

    @Parameter(value = "elasticsearch_max_size_per_index", validator = PositiveLongValidator.class, required = true)
    private long maxSizePerIndex = 1L * 1024 * 1024 * 1024; // 1GB

    @Parameter(value = "elasticsearch_max_time_per_index", required = true)
    private Period maxTimePerIndex = Period.days(1);

    @Parameter(value = "elasticsearch_shards", validator = PositiveIntegerValidator.class, required = true)
    private int shards = 4;

    @Parameter(value = "elasticsearch_replicas", validator = PositiveIntegerValidator.class, required = true)
    private int replicas = 0;

    @Parameter(value = "elasticsearch_analyzer", required = true)
    private String analyzer = "standard";

    @Parameter(value = "no_retention")
    private boolean noRetention = false;

    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy = "delete";

    @Parameter(value = "rotation_strategy")
    private String rotationStrategy = "count";

    @Parameter(value = "disable_index_optimization")
    private boolean disableIndexOptimization = false;

    @Parameter(value = "index_optimization_max_num_segments", validator = PositiveIntegerValidator.class)
    private int indexOptimizationMaxNumSegments = 1;

    @Parameter(value = "elasticsearch_request_timeout", validator = PositiveDurationValidator.class)
    private Duration requestTimeout = Duration.minutes(1L);

    public String getClusterName() {
        return clusterName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean isMasterNode() {
        return masterNode;
    }

    public boolean isDataNode() {
        return dataNode;
    }

    public boolean isClientNode() {
        return !isDataNode();
    }

    public int getTransportTcpPort() {
        return transportTcpPort;
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public boolean isMulticastDiscovery() {
        return multicastDiscovery;
    }

    public List<String> getUnicastHosts() {
        return unicastHosts;
    }

    public String getInitialStateTimeout() {
        return initialStateTimeout;
    }

    public String getNetworkHost() {
        return networkHost;
    }

    public String getNetworkBindHost() {
        return networkBindHost;
    }

    public String getNetworkPublishHost() {
        return networkPublishHost;
    }

    public long getClusterDiscoveryTimeout() {
        return clusterDiscoveryTimeout;
    }

    public boolean isDisableVersionCheck() {
        return disableVersionCheck;
    }

    public File getConfigFile() {
        return configFile;
    }

    public String getIndexPrefix() {
        return indexPrefix.toLowerCase(Locale.ENGLISH);
    }

    public int getMaxNumberOfIndices() {
        return maxNumberOfIndices;
    }

    public int getMaxDocsPerIndex() {
        return maxDocsPerIndex;
    }

    public long getMaxSizePerIndex() {
        return maxSizePerIndex;
    }

    public Period getMaxTimePerIndex() {
        return maxTimePerIndex;
    }

    public int getShards() {
        return shards;
    }

    public int getReplicas() {
        return replicas;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public String getRotationStrategy() {
        return rotationStrategy;
    }

    public boolean performRetention() {
        return !noRetention;
    }

    public void setPerformRetention(boolean retention) {
        noRetention = !retention;
    }

    public String getRetentionStrategy() {
        return retentionStrategy;
    }

    public int getIndexOptimizationMaxNumSegments() {
        return indexOptimizationMaxNumSegments;
    }

    public boolean isDisableIndexOptimization() {
        return disableIndexOptimization;
    }

    public String getPathData() {
        return pathData;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }
}
