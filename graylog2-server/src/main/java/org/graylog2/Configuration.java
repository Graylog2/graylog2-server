/**
 * Copyright 2010, 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.validators.FileReadableValidator;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.google.common.collect.Lists;
import com.lmax.disruptor.*;
import com.mongodb.ServerAddress;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class to hold configuration of Graylog2
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @Parameter(value = "is_master", required = true)
    private boolean isMaster = true;

    @Parameter(value = "password_secret", required = true)
    private String passwordSecret;
    
    @Parameter(value = "rest_listen_uri", required = true)
    private String restListenUri = "http://127.0.0.1:12900/";

    @Parameter(value = "rest_transport_uri", required = false)
    private String restTransportUri;

    @Parameter(value = "udp_recvbuffer_sizes", required = true, validator = PositiveIntegerValidator.class)
    private int udpRecvBufferSizes = 1048576;
    
    @Parameter(value = "force_syslog_rdns", required = true)
    private boolean forceSyslogRdns = false;

    @Parameter(value = "mongodb_useauth", required = true)
    private boolean mongoUseAuth = false;

    @Parameter(value = "allow_override_syslog_date", required = true)
    private boolean allowOverrideSyslogDate = true;

    @Parameter(value = "no_retention")
    private boolean noRetention;

    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy;
    
    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validator = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    @Parameter(value = "output_batch_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBatchSize = 5000;
    
    @Parameter(value = "processbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int processBufferProcessors = 5;
    
    @Parameter(value = "outputbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessors = 5;
    
    @Parameter(value = "outputbuffer_processor_threads_max_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsMaxPoolSize = 30;
    
    @Parameter(value = "outputbuffer_processor_threads_core_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsCorePoolSize = 3;
    
    @Parameter(value = "processor_wait_strategy", required = true)
    private String processorWaitStrategy = "blocking";
    
    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 1024;

    @Parameter(value = "elasticsearch_config_file", required = false, validator = FileReadableValidator.class)
    private String elasticSearchConfigFile; // = "/etc/graylog2-elasticsearch.yml";

    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String elasticsearchIndexPrefix = "graylog2";
    
    @Parameter(value = "elasticsearch_max_docs_per_index", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchMaxDocsPerIndex = 80000000;
    
    @Parameter(value = "elasticsearch_shards", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchShards = 4;
    
    @Parameter(value = "elasticsearch_replicas", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchReplicas = 0;
    
    @Parameter(value = "elasticsearch_analyzer", required = true)
    private String elasticsearchAnalyzer = "standard";
    
    @Parameter(value = "mongodb_user")
    private String mongoUser;

    @Parameter(value = "mongodb_password")
    private String mongoPassword;

    @Parameter(value = "mongodb_database", required = true)
    private String mongoDatabase = "graylog2";

    @Parameter(value = "mongodb_host", required = true)
    private String mongoHost = "127.0.0.1";

    @Parameter(value = "mongodb_port", required = true, validator = InetPortValidator.class)
    private int mongoPort = 27017;

    @Parameter(value = "mongodb_max_connections", validator = PositiveIntegerValidator.class)
    private int mongoMaxConnections = 1000;

    @Parameter(value = "mongodb_threads_allowed_to_block_multiplier", validator = PositiveIntegerValidator.class)
    private int mongoThreadsAllowedToBlockMultiplier = 5;

    @Parameter(value = "mongodb_replica_set", converter = StringListConverter.class)
    private List<String> mongoReplicaSet;

    @Parameter("rules_file")
    private String droolsRulesFile;

    @Parameter(value = "enable_graphite_output", required = false)
    private boolean enableGraphiteOutput = false;

    @Parameter(value = "graphite_carbon_host", required = false)
    private String graphiteCarbonHost = "127.0.0.1";

    @Parameter(value = "graphite_carbon_tcp_port", validator = InetPortValidator.class, required = false)
    private int graphiteCarbonTcpPort = 2003;
    
    @Parameter(value = "graphite_prefix", required = false)
    private String graphitePrefix = "graylog2-server";

    @Parameter(value = "enable_libratometrics_output", required = false)
    private boolean enableLibratoMetricsOutput = false;
    
    @Parameter(value = "enable_libratometrics_system_metrics", required = false)
    private boolean enableLibratoSystemMetrics = false;

    @Parameter(value = "libratometrics_api_user", required = false)
    private String libratometricsApiUser;

    @Parameter(value = "libratometrics_api_token", required = false)
    private String libratometricsApiToken;

    @Parameter(value = "libratometrics_stream_filter", required = false)
    private String libratometricsStreamFilter = "";

    @Parameter(value = "libratometrics_host_filter", required = false)
    private String libratometricsHostFilter = "";

    @Parameter(value = "libratometrics_interval", validator = PositiveIntegerValidator.class, required = false)
    private int libratometricsInterval = 10;

    @Parameter(value = "libratometrics_prefix", required = false)
    private String libratometricsPrefix = "gl2-";

    @Parameter(value = "plugin_dir", required = false)
    private String pluginDir = "plugin";

    @Parameter(value = "node_id_file", required = false)
    private String nodeIdFile = "/etc/graylog2-server-node-id";

    @Parameter(value = "root_username", required = false)
    private String rootUsername = "admin";

    @Parameter(value = "root_password_sha2", required = true)
    private String rootPasswordSha2;

    @Parameter(value = "allow_leading_wildcard_searches", required = false)
    private boolean allowLeadingWildcardSearches = false;

    /* Elasticsearch defaults */
    @Parameter(value = "elasticsearch_cluster_name", required = true)
    private String esClusterName = "graylog2";

    @Parameter(value = "elasticsearch_node_name", required = true)
    private String esNodeName = "graylog2-server";

    @Parameter(value = "elasticsearch_node_master", required = true)
    private boolean esIsMasterEligible = false;

    @Parameter(value = "elasticsearch_node_data", required = true)
    private boolean esStoreData = false;

    @Parameter(value = "elasticsearch_transport_tcp_port", validator = InetPortValidator.class, required = true)
    private int esTransportTcpPort = 9350;

    @Parameter(value = "elasticsearch_http_enabled", required = false)
    private boolean esIsHttpEnabled = false;

    @Parameter(value = "elasticsearch_discovery_zen_ping_multicast_enabled", required = false)
    private boolean esMulticastDiscovery = true;

    @Parameter(value = "elasticsearch_discovery_zen_ping_unicast_hosts", required = false, converter = StringListConverter.class)
    private List<String> esUnicastHosts;

    public boolean isMaster() {
        return isMaster;
    }
    
    public void setIsMaster(boolean is) {
        isMaster = is;
    }

    public String getPasswordSecret() {
        return passwordSecret.trim();
    }

    public boolean performRetention() {
        return !noRetention;
    }
    
    public int getMaxNumberOfIndices() {
        return maxNumberOfIndices;
    }

    public int getOutputBatchSize() {
        return outputBatchSize;
    }
    
    public int getProcessBufferProcessors() {
        return processBufferProcessors;
    }
    
    public int getOutputBufferProcessors() {
        return outputBufferProcessors;
    }
    
    public int getOutputBufferProcessorThreadsCorePoolSize() {
        return outputBufferProcessorThreadsCorePoolSize;
    }
    
    public int getOutputBufferProcessorThreadsMaxPoolSize() {
        return outputBufferProcessorThreadsMaxPoolSize;
    }
    
    public int getUdpRecvBufferSizes() {
        return udpRecvBufferSizes;
    }
    
    public WaitStrategy getProcessorWaitStrategy() {
        if (processorWaitStrategy.equals("sleeping")) {
            return new SleepingWaitStrategy();
        }
        
        if (processorWaitStrategy.equals("yielding")) {
            return new YieldingWaitStrategy();
        }
        
        if (processorWaitStrategy.equals("blocking")) {
            return new BlockingWaitStrategy();
        }
        
        if (processorWaitStrategy.equals("busy_spinning")) {
            return new BusySpinWaitStrategy();
        }
        
        LOG.warn("Invalid setting for [processor_wait_strategy]:"
                + " Falling back to default: BlockingWaitStrategy.");
        return new BlockingWaitStrategy();
    }

    public int getRingSize() {
        return ringSize;
    }
    
    public String getElasticSearchConfigFile() {
        return elasticSearchConfigFile;
    }

    public String getElasticSearchIndexPrefix() {
        return this.elasticsearchIndexPrefix;
    }
    
    public int getElasticSearchMaxDocsPerIndex() {
        return this.elasticsearchMaxDocsPerIndex;
    }

    public int getElasticSearchShards() {
        return this.elasticsearchShards;
    }
    
    public int getElasticSearchReplicas() {
        return this.elasticsearchReplicas;
    }
    
    public String getElasticSearchAnalyzer() {
        return elasticsearchAnalyzer;
    }
   
    public boolean isMongoUseAuth() {
        return mongoUseAuth;
    }

    public String getMongoUser() {
        return mongoUser;
    }

    public String getMongoPassword() {
        return mongoPassword;
    }

    public String getMongoDatabase() {
        return mongoDatabase;
    }

    public int getMongoPort() {
        return mongoPort;
    }

    public String getMongoHost() {
        return mongoHost;
    }

    public int getMongoMaxConnections() {
        return mongoMaxConnections;
    }

    public int getMongoThreadsAllowedToBlockMultiplier() {
        return mongoThreadsAllowedToBlockMultiplier;
    }

    public String getDroolsRulesFile() {
        return droolsRulesFile;
    }

    public List<ServerAddress> getMongoReplicaSet() {
        List<ServerAddress> replicaServers = Lists.newArrayList();

        List<String> rawSet = mongoReplicaSet;

        if (rawSet == null || rawSet.isEmpty()) {
            return null;
        }

        for (String host : rawSet) {
            // Split host:port.
            String[] replicaTarget = host.split(":");

            // Check if valid.
            if (replicaTarget == null || replicaTarget.length != 2) {
                LOG.error("Malformed mongodb_replica_set configuration.");
                return null;
            }

            // Get host and port.
            try {
                replicaServers.add(new ServerAddress(replicaTarget[0], Integer.parseInt(replicaTarget[1])));
            } catch (UnknownHostException e) {
                LOG.error("Unknown host in mongodb_replica_set: " + e.getMessage(), e);
                return null;
            }
        }

        return replicaServers;
    }

    public boolean isEnableGraphiteOutput() {
        return enableGraphiteOutput;
    }

    public String getGraphiteCarbonHost() {
        return graphiteCarbonHost;
    }

    public int getGraphiteCarbonTcpPort() {
        return graphiteCarbonTcpPort;
    }
    
    public String getGraphitePrefix() {
        return graphitePrefix;
    }

    public boolean isEnableLibratoMetricsOutput() {
        return enableLibratoMetricsOutput;
    }

    public boolean isEnableLibratoSystemMetrics() {
        return enableLibratoSystemMetrics;
    }
    
    public String getLibratoMetricsAPIUser() {
        return libratometricsApiUser;
    }

    public String getLibratoMetricsAPIToken() {
        return libratometricsApiToken;
    }

    public List<String> getLibratoMetricsStreamFilter() {
        List<String> r = Lists.newArrayList();
        r.addAll(Arrays.asList(libratometricsStreamFilter.split(",")));

        return r;
    }

    public String getLibratoMetricsHostsFilter() {
        return libratometricsHostFilter;
    }

    public int getLibratoMetricsInterval() {
        return libratometricsInterval;
    }

    public String getLibratoMetricsPrefix() {
        return libratometricsPrefix;
    }

    public String getPluginDir() {
        return pluginDir;
    }

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    @ValidatorMethod
    public void validate() throws ValidationException {

        if (isMongoUseAuth() && (null == getMongoUser() || null == getMongoPassword())) {
            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }
    }

    public URI getRestListenUri() {
        return Tools.getUriStandard(restListenUri);
    }

    public URI getRestTransportUri() {
        if (restTransportUri == null || restTransportUri.isEmpty()) {
            return null;
        }

        return Tools.getUriStandard(restTransportUri);
    }

    public String getRootUsername() {
        return rootUsername;
    }

    public String getRootPasswordSha2() {
        return rootPasswordSha2;
    }

    public void setRestTransportUri(String restTransportUri) {
        this.restTransportUri = restTransportUri;
    }

    public String getEsClusterName() {
        return esClusterName;
    }

    public String getEsNodeName() {
        return esNodeName;
    }

    public boolean isEsIsMasterEligible() {
        return esIsMasterEligible;
    }

    public boolean isEsStoreData() {
        return esStoreData;
    }

    public int getEsTransportTcpPort() {
        return esTransportTcpPort;
    }

    public boolean isEsIsHttpEnabled() {
        return esIsHttpEnabled;
    }

    public boolean isEsMulticastDiscovery() {
        return esMulticastDiscovery;
    }

    public List<String> getEsUnicastHosts() {
        return esUnicastHosts;
    }

    public String getRetentionStrategy() {
        return retentionStrategy;
    }

    public boolean isAllowLeadingWildcardSearches() {
        return allowLeadingWildcardSearches;
    }
}
